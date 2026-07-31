package com.ezdo.service.ai.plan;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.dto.ai.AiUserPreferencesContext;
import com.ezdo.dto.ai.plan.DraftedSession;
import com.ezdo.dto.ai.plan.DraftedTask;
import com.ezdo.dto.ai.plan.ProposedTask;
import com.ezdo.dto.ai.plan.ScheduleContext;
import com.ezdo.dto.ai.plan.TaskPlanResult;
import com.ezdo.dto.ai.plan.TaskPlanningRequest;
import com.ezdo.dto.ai.plan.TaskProposalResponse;
import com.ezdo.dto.goal.TaskCreateRequest;
import com.ezdo.dto.task.SessionDraftRequest;
import com.ezdo.dto.task.TaskWithSessionsRequest;
import com.ezdo.entity.SessionStatus;
import com.ezdo.exception.AiUnavailableException;
import com.ezdo.exception.ResultParseException;
import com.ezdo.service.ai.ScheduleContextService;
import com.ezdo.service.ai.TaskDraftNormalizer;
import com.ezdo.service.ai.UserContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Turns a source text describing things a user wants to do into a list of proposed
 * tasks. Nothing is persisted — the result is a proposal the user reviews and then
 * confirms task-by-task against {@code POST /api/v1/tasks/with-sessions}.
 *
 * <p>Two front-ends share this one planner: a note the user typed, and the report
 * the vision model produces from an uploaded image. The contract is identical either
 * way — the model owns every field including the title, and returns N tasks rather
 * than one, because "gym Mon &amp; Wed 6-8pm" is the same problem whether it was
 * typed or photographed.
 *
 * <p>Each task comes back with two independent sets of sessions. Timing the source
 * itself stated is passed through untouched; alongside it the model proposes when it
 * would schedule the task, grounded in the user's real availability, with a reason.
 * They stay separate so the user's explicit wording is never silently overruled.
 */
@Slf4j
@Service
public class TaskPlanningService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    /** Used only if the model ignores the contract and omits its reason. */
    private static final String MISSING_REASON = "No explanation was provided for this suggestion.";

    private final UserContextService userContextService;
    private final ScheduleContextService scheduleContextService;
    private final TaskDraftNormalizer normalizer;
    private final ChatClient planningClient;
    private final TaskPlanningPromptBuilder promptBuilder;
    private final TaskPlanningCodec codec;
    private final int maxTasks;
    private final int maxSessionsPerTask;
    private final int horizonDays;

    public TaskPlanningService(
        UserContextService userContextService,
        ScheduleContextService scheduleContextService,
        TaskDraftNormalizer normalizer,
        @Qualifier("planningModel") ChatClient planningClient,
        TaskPlanningPromptBuilder promptBuilder,
        TaskPlanningCodec codec,
        @Value("${ezdo.ai.plan.max-tasks}") int maxTasks,
        @Value("${ezdo.ai.plan.max-sessions-per-task}") int maxSessionsPerTask,
        @Value("${ezdo.ai.plan.availability-horizon-days}") int horizonDays
    ) {
        this.userContextService = userContextService;
        this.scheduleContextService = scheduleContextService;
        this.normalizer = normalizer;
        this.planningClient = planningClient;
        this.promptBuilder = promptBuilder;
        this.codec = codec;
        this.maxTasks = maxTasks;
        this.maxSessionsPerTask = maxSessionsPerTask;
        this.horizonDays = horizonDays;
    }

    /**
     * Front-end for typed text. There is nothing to summarise for the UI here — the
     * user can already see what they wrote — so {@code sourceSummary} is null.
     */
    public TaskProposalResponse planFromText(UUID userId, TaskPlanningRequest request) {
        AiUserPreferencesContext context = userContextService.buildFor(userId);
        List<ProposedTask> tasks =
            plan(userId, request.text().strip(), SourceKind.TYPED_NOTE, context);
        return new TaskProposalResponse(null, tasks, Instant.now());
    }

    /**
     * The shared core. Callers that already built the user context pass it in, so a
     * request never builds it twice.
     */
    public List<ProposedTask> plan(UUID userId,
                                   String sourceText,
                                   SourceKind kind,
                                   AiUserPreferencesContext context) {
        ScheduleContext schedule = scheduleContextService.buildFor(userId, context, horizonDays);
        List<Message> messages = promptBuilder.build(sourceText, kind, context, schedule);
        return toDrafts(generateResult(messages, userId), userId, context, schedule);
    }

    // --- model call ----------------------------------------------------------

    /**
     * Parse failures get one retry, the same policy as the other AI flows. A second
     * failure has no sensible partial answer to degrade to, so it surfaces as
     * {@link AiUnavailableException}.
     */
    private TaskPlanResult generateResult(List<Message> messages, UUID userId) {
        try {
            return codec.parseResult(callModel(messages, userId));
        } catch (ResultParseException first) {
            log.info("Task planning reply was not valid JSON, retrying once", first);
            try {
                return codec.parseResult(callModel(messages, userId));
            } catch (ResultParseException second) {
                throw new AiUnavailableException(second);
            }
        }
    }

    private String callModel(List<Message> messages, UUID userId) {
        try {
            return planningClient.prompt()
                .messages(messages)
                .advisors(a -> a.param("userId", userId.toString()))
                .call()
                .content();
        } catch (Exception e) {
            log.error("", e);
            throw new AiUnavailableException(e);
        }
    }

    // --- draft mapping -------------------------------------------------------

    /**
     * Every field here comes from a model, so every field is re-checked: caps are
     * enforced server-side rather than trusted to the prompt, category ids are
     * matched against what the user really owns, and impossible sessions are dropped
     * instead of being handed to the client as a request it cannot post back.
     */
    private List<ProposedTask> toDrafts(TaskPlanResult result,
                                        UUID userId,
                                        AiUserPreferencesContext context,
                                        ScheduleContext schedule) {
        List<DraftedTask> tasks = result.tasks() != null ? result.tasks() : List.of();
        if (tasks.size() > maxTasks) {
            log.warn("Task planning for user {} returned {} tasks; keeping the first {}",
                userId, tasks.size(), maxTasks);
            tasks = tasks.subList(0, maxTasks);
        }

        Set<UUID> validCategoryIds = normalizer.validCategoryIds();
        List<LockedInterval> locked = lockedIntervals(schedule);
        List<ProposedTask> drafts = new ArrayList<>();

        for (DraftedTask task : tasks) {
            if (task == null) {
                continue;
            }
            String title = normalizer.truncate(task.title(), MAX_TITLE_LENGTH);
            if (title == null) {
                log.warn("Task planning for user {} produced a task with no title; dropping it", userId);
                continue;
            }

            CategoryResponse category =
                normalizer.sanitizeCategory(task.category(), validCategoryIds);

            TaskCreateRequest createRequest = new TaskCreateRequest(
                title,
                normalizer.truncate(task.description(), MAX_DESCRIPTION_LENGTH),
                normalizer.normalizeDuration(task.estimatedDuration()),
                task.mandatory() == null || task.mandatory(),
                normalizer.normalizePoints(task.estimatedPoints()),
                task.allowTaskSplitting() != null && task.allowTaskSplitting(),
                null,
                category != null ? category.id() : null
            );

            // The user asked for these times explicitly, so they pass through as given.
            List<SessionDraftRequest> stated = toSessions(task.sessions(), title, userId, schedule);

            // The model chose these, so they are checked against the same calendar it was shown.
            List<SessionDraftRequest> proposed = validateProposed(
                toSessions(task.proposedSessions(), title, userId, schedule),
                title, userId, context, locked);

            drafts.add(new ProposedTask(
                new TaskWithSessionsRequest(createRequest, stated),
                proposed,
                resolveReason(task, userId)
            ));
        }
        return drafts;
    }

    private List<SessionDraftRequest> toSessions(List<DraftedSession> sessions,
                                                 String taskTitle,
                                                 UUID userId,
                                                 ScheduleContext schedule) {
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }
        List<SessionDraftRequest> result = new ArrayList<>();
        for (DraftedSession session : sessions) {
            if (session == null || session.start() == null || session.end() == null
                || !session.end().isAfter(session.start())) {
                log.warn("Task planning for user {} produced an invalid session for task '{}'; dropping it",
                    userId, taskTitle);
                continue;
            }
            if (result.size() == maxSessionsPerTask) {
                log.warn("Task planning for user {} produced more than {} sessions for task '{}'; truncating",
                    userId, maxSessionsPerTask, taskTitle);
                break;
            }
            result.add(new SessionDraftRequest(
                zoneIdFor(session.start(), session.end(), schedule),
                session.start(), session.end(), SessionStatus.SCHEDULED));
        }
        return result;
    }

    /**
     * Which zone a session falls in, resolved here rather than asked of the model:
     * free slots never overlap, so the chosen time already determines the zone, and
     * making the model echo a UUID would only add a way for it to be wrong.
     *
     * <p>Requires the slot to fully contain the session — a session straddling two
     * slots, sitting outside every slot, or landing in an unzoned gap resolves to
     * null. {@code Session.zone} is nullable and handled everywhere, so claiming no
     * zone is always safe; claiming the wrong one would not be.
     */
    private UUID zoneIdFor(LocalDateTime start, LocalDateTime end, ScheduleContext schedule) {
        if (schedule == null || schedule.days() == null) {
            return null;
        }
        LocalDate date = start.toLocalDate();
        if (!end.toLocalDate().equals(date)) {
            return null; // spans midnight, so no single day's slots can contain it
        }
        for (ScheduleContext.ScheduleDay day : schedule.days()) {
            if (!day.date().equals(date) || day.free() == null) {
                continue;
            }
            for (ScheduleContext.FreeSlot slot : day.free()) {
                if (slot.zoneId() == null) {
                    continue;
                }
                LocalDateTime slotStart = date.atTime(slot.start());
                LocalDateTime slotEnd = date.atTime(slot.end());
                if (!start.isBefore(slotStart) && !end.isAfter(slotEnd)) {
                    return slot.zoneId();
                }
            }
            return null; // the day was found; no slot contains this session
        }
        return null;
    }

    /**
     * The model's own suggestions get checked against the calendar it was shown.
     * Overlapping an ordinary booked session is allowed — that is the deliberate
     * least-cost conflict the prompt asks for, and the reason explains it. A locked
     * session is different: it is immovable, so a suggestion sitting on one is
     * something the user could never act on, and dropping it beats offering it.
     */
    private List<SessionDraftRequest> validateProposed(List<SessionDraftRequest> sessions,
                                                       String taskTitle,
                                                       UUID userId,
                                                       AiUserPreferencesContext context,
                                                       List<LockedInterval> locked) {
        if (sessions.isEmpty()) {
            return sessions;
        }
        List<SessionDraftRequest> kept = new ArrayList<>();
        for (SessionDraftRequest session : sessions) {
            if (session.start().isBefore(context.now())) {
                log.warn("Task planning for user {} proposed a session in the past for task '{}'; dropping it",
                    userId, taskTitle);
                continue;
            }
            if (overlapsLocked(session, locked)) {
                log.warn("Task planning for user {} proposed a session overlapping a locked session "
                    + "for task '{}'; dropping it", userId, taskTitle);
                continue;
            }
            kept.add(session);
        }
        return kept;
    }

    private boolean overlapsLocked(SessionDraftRequest session, List<LockedInterval> locked) {
        return locked.stream().anyMatch(l ->
            session.start().isBefore(l.end()) && l.start().isBefore(session.end()));
    }

    private List<LockedInterval> lockedIntervals(ScheduleContext schedule) {
        List<LockedInterval> intervals = new ArrayList<>();
        if (schedule == null || schedule.days() == null) {
            return intervals;
        }
        for (ScheduleContext.ScheduleDay day : schedule.days()) {
            if (day.booked() == null) {
                continue;
            }
            for (ScheduleContext.BookedItem item : day.booked()) {
                if (!item.locked()) {
                    continue;
                }
                LocalDateTime start = day.date().atTime(item.start());
                LocalDateTime end = day.date().atTime(item.end());
                if (!end.isAfter(start)) {
                    end = end.plusDays(1); // the session runs past midnight
                }
                intervals.add(new LockedInterval(start, end));
            }
        }
        return intervals;
    }

    private String resolveReason(DraftedTask task, UUID userId) {
        if (task.reason() == null || task.reason().isBlank()) {
            log.warn("Task planning for user {} returned no reason for task '{}'", userId, task.title());
            return MISSING_REASON;
        }
        return task.reason().strip();
    }

    private record LockedInterval(LocalDateTime start, LocalDateTime end) {}
}
