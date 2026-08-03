package com.ezdo.service;

import com.ezdo.dto.ai.schedule.*;
import com.ezdo.entity.Session;
import com.ezdo.entity.Task;
import com.ezdo.entity.Zone;
import com.ezdo.exception.TaskNotFoundException;
import com.ezdo.exception.ZoneNotFoundException;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.TaskRepository;
import com.ezdo.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI-driven goal scheduler.
 *
 * <h3>Persistence model</h3>
 * <strong>This service never writes to the database.</strong> The scheduling
 * call returns a fully formed proposal that the user reviews and selects from.
 * Only {@link #confirmSchedule} actually persists sessions, once the client
 * sends back the sessions it wants to save.
 *
 * <h3>Result structure</h3>
 * <ol>
 *   <li>{@code proposedSessions} — sessions the AI placed inside a correctly
 *       matched zone with sufficient free time. Ready to confirm as-is.</li>
 *   <li>{@code suggestions} — sessions the AI placed outside a matched zone
 *       ({@link SuggestionType#NO_ZONE}) or overlapping an existing unlocked
 *       session ({@link SuggestionType#OVERLAP}). The client shows these to
 *       the user who decides whether to accept them.</li>
 *   <li>{@code unscheduledTasks} — tasks the AI could not place anywhere, not
 *       even with a suggestion (e.g. only locked sessions remain).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AISchedulingService {

    private final AiContextBuilder contextBuilder;
    private final AiSchedulerClient schedulerClient;
    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final ZoneRepository zoneRepository;

    /**
     * Asks the AI to schedule all tasks in the goal and returns the complete
     * proposal without persisting anything.
     */
    public AiGoalScheduleResponse scheduleGoal(UUID userId, GoalScheduleRequest request) {
        AiSchedulingPayload payload = contextBuilder.buildPayload(userId, request.goalId(), 14);
        AiSchedulingResult result   = schedulerClient.scheduleTasks(payload);

        log.info("AI scheduling result for goal {}: {} sessions, {} failed",
                request.goalId(), result.sessions().size(), result.failedTasks().size());

        return buildResponse(request.goalId(), result, userId, payload);
    }

    // ── confirm (persists the user-selected sessions) ────────────────────────

    /**
     * Persists the sessions the user selected from the scheduling proposal.
     * Each {@link ConfirmSessionItem} is validated (task ownership, optional
     * zone ownership) and saved as a {@link Session}.
     *
     * @return the list of persisted sessions (with real IDs).
     */
    @Transactional
    public List<ScheduledSessionResult> confirmSchedule(UUID userId, ConfirmScheduleRequest request) {
        List<ScheduledSessionResult> saved = new ArrayList<>();

        for (ConfirmSessionItem item : request.sessions()) {
            Task task = taskRepository.findById(item.taskId())
                    .filter(t -> t.getGoal() != null && t.getGoal().getUser() != null
                                 && userId.equals(t.getGoal().getUser().getId()))
                    .orElseThrow(() -> new TaskNotFoundException(item.taskId()));

            Zone zone = null;
            if (item.zoneId() != null) {
                zone = zoneRepository.findByIdAndUserId(item.zoneId(), userId)
                        .orElseThrow(() -> new ZoneNotFoundException(item.zoneId()));
            }

            Session session = Session.builder()
                    .start(item.start())
                    .end(item.end())
                    .task(task)
                    .zone(zone)
                    .build();

            sessionRepository.save(session);

            saved.add(new ScheduledSessionResult(
                    session.getId(),
                    item.taskId(),
                    item.zoneId(),
                    session.getStart(),
                    session.getEnd()
            ));
        }

        return saved;
    }

    // ── response assembly ────────────────────────────────────────────────────

    /**
     * Splits the AI result into the three response buckets:
     * <ul>
     *   <li>Normal proposed sessions (in-zone, no conflict)</li>
     *   <li>Suggestions (no-zone or soft overlap)</li>
     *   <li>Failed tasks</li>
     * </ul>
     *
     * <p>Zone resolution for normal sessions is done server-side by matching
     * the AI's chosen start/end against the free-slot table — the AI is not
     * trusted to echo a UUID back correctly. For no-zone suggestions the zone
     * stays {@code null}.
     */
    private AiGoalScheduleResponse buildResponse(UUID goalId,
                                               AiSchedulingResult result,
                                               UUID userId,
                                               AiSchedulingPayload payload) {
        List<ProposedSessionResult> proposed   = new ArrayList<>();
        List<SuggestedSession>      suggestions = new ArrayList<>();

        // Index booked sessions by date so we can look up OverlapInfo efficiently
        Map<LocalDate, List<BookedSessionItem>> bookedByDate = payload.calendar().stream()
                .collect(Collectors.toMap(
                        DaySlot::date,
                        DaySlot::bookedSessions
                ));

        for (ProposedSession ps : result.sessions()) {
            UUID taskId = UUID.fromString(ps.taskId());
            String suggestionType = ps.suggestionType();

            if ("NO_ZONE".equals(suggestionType)) {
                // ── no-zone suggestion ───────────────────────────────────────
                String taskTitle = resolveTaskTitle(taskId);
                suggestions.add(new SuggestedSession(
                        taskId,
                        taskTitle,
                        null,               // no zone
                        ps.startTime(),
                        ps.endTime(),
                        SuggestionType.NO_ZONE,
                        ps.reason(),
                        null                // no overlap info
                ));

            } else if ("OVERLAP".equals(suggestionType)) {
                // ── overlap suggestion ───────────────────────────────────────
                String taskTitle  = resolveTaskTitle(taskId);
                UUID   zoneId     = resolveZoneId(ps, payload);
                OverlapInfo overlap = findOverlapInfo(
                        ps.startTime(), ps.endTime(),
                        bookedByDate.getOrDefault(ps.startTime().toLocalDate(), List.of()));

                suggestions.add(new SuggestedSession(
                        taskId,
                        taskTitle,
                        zoneId,
                        ps.startTime(),
                        ps.endTime(),
                        SuggestionType.OVERLAP,
                        ps.reason(),
                        overlap
                ));

            } else {
                // ── normal in-zone proposed session ──────────────────────────
                // Resolve the zone server-side from the free-slot table instead
                // of trusting the AI to echo a valid UUID.
                UUID zoneId    = resolveZoneId(ps, payload);
                String taskTitle = resolveTaskTitle(taskId);

                proposed.add(new ProposedSessionResult(
                        taskId,
                        taskTitle,
                        zoneId,
                        ps.startTime(),
                        ps.endTime()
                ));
            }
        }

        // ── failed tasks ─────────────────────────────────────────────────────
        List<UnscheduledTaskResult> failed = result.failedTasks().stream()
                .map(f -> {
                    UUID taskId = UUID.fromString(f.taskId());
                    return new UnscheduledTaskResult(
                            taskId,
                            resolveTaskTitle(taskId),
                            f.message()
                    );
                })
                .toList();

        return new AiGoalScheduleResponse(goalId, proposed, suggestions, failed);
    }

    // ── zone resolution ───────────────────────────────────────────────────────

    /**
     * Resolves which zone the AI's chosen time falls into by scanning the
     * free-slot table that was sent to the AI — the same approach used by
     *
     * <p>A slot must <em>fully contain</em> the session (not merely overlap it)
     * because a session straddling two zones would be ambiguous. Returns
     * {@code null} when no slot contains the session or when it spans midnight.
     */
    private UUID resolveZoneId(ProposedSession ps, AiSchedulingPayload payload) {
        if (ps.startTime() == null || ps.endTime() == null) return null;

        LocalDate date = ps.startTime().toLocalDate();
        if (!ps.endTime().toLocalDate().equals(date)) return null; // spans midnight

        for (DaySlot day : payload.calendar()) {
            if (!day.date().equals(date)) continue;
            for (FreeSlotItem slot : day.freeSlots()) {
                if (slot.zoneId() == null) continue;
                LocalDateTime slotStart = date.atTime(slot.start());
                LocalDateTime slotEnd   = date.atTime(slot.end());
                if (!ps.startTime().isBefore(slotStart) && !ps.endTime().isAfter(slotEnd)) {
                    return slot.zoneId();
                }
            }
            return null; // day found but no slot contains this session
        }
        return null;
    }

    // ── overlap info ──────────────────────────────────────────────────────────

    /**
     * Finds the booked session that overlaps the proposed window and returns
     * its cost signals for the client to display. Returns {@code null} when
     * the booked list is empty or nothing overlaps (defensive fallback).
     */
    private OverlapInfo findOverlapInfo(LocalDateTime start,
                                        LocalDateTime end,
                                        List<BookedSessionItem> booked) {
        LocalDate date = start.toLocalDate();
        for (BookedSessionItem b : booked) {
            LocalDateTime bStart = date.atTime(b.start());
            LocalDateTime bEnd   = date.atTime(b.end());
            // any overlap: bStart < end AND start < bEnd
            if (bStart.isBefore(end) && start.isBefore(bEnd)) {
                return new OverlapInfo(b.taskTitle(), bStart, bEnd, b.mandatory(), b.points());
            }
        }
        return null;
    }

    // ── task title helper ─────────────────────────────────────────────────────

    private String resolveTaskTitle(UUID taskId) {
        return taskRepository.findById(taskId)
                .map(Task::getTitle)
                .orElse("Unknown Task");
    }
}