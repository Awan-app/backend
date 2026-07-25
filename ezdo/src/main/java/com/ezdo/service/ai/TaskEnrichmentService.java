package com.ezdo.service.ai;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.dto.ai.decompose.DecompositionUserContext;
import com.ezdo.dto.ai.enrich.TaskEnrichmentRequest;
import com.ezdo.dto.ai.enrich.TaskEnrichmentResult;
import com.ezdo.dto.goal.TaskCreateRequest;
import com.ezdo.dto.task.SessionDraftRequest;
import com.ezdo.dto.task.TaskWithSessionsRequest;
import com.ezdo.dto.task.TaskWithSessionsResponse;
import com.ezdo.entity.Category;
import com.ezdo.entity.SessionStatus;
import com.ezdo.exception.AiUnavailableException;
import com.ezdo.exception.InvalidOperationException;
import com.ezdo.repository.CategoryRepository;
import com.ezdo.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Single-shot enrichment: given a task title/description, asks the model to fill in
 * the operational fields (description if blank, duration, points, mandatory,
 * splitting, category, and optionally scheduling info) grounded in the user's real
 * categories and scheduling preferences. Creates the Task and, if the user mentioned
 * timing, also creates Sessions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskEnrichmentService {

    private static final int DEFAULT_ESTIMATED_DURATION_MINUTES = 30;

    private final UserContextService userContextService;
    private final CategoryRepository categoryRepository;
    private final ChatClient chatClient;
    private final TaskEnrichmentPromptBuilder promptBuilder;
    private final TaskEnrichmentCodec codec;
    private final TaskService taskService;

    public TaskWithSessionsResponse enrich(UUID userId, TaskEnrichmentRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new InvalidOperationException("Task title is required");
        }

        DecompositionUserContext context = userContextService.buildFor(userId);
        List<Message> messages = promptBuilder.build(request, context);

        TaskEnrichmentResult result = generateResult(messages, userId);
        CategoryResponse category = sanitizeCategory(result.category(), userId);

        TaskCreateRequest createRequest = new TaskCreateRequest(
            request.title().strip(),
            resolveDescription(request, result),
            normalizeDuration(result.estimatedDuration()),
            result.mandatory() == null || result.mandatory(),
            normalizePoints(result.estimatedPoints()),
            result.allowTaskSplitting() != null && result.allowTaskSplitting(),
            null,
            category != null ? category.id() : null
        );

        if (result.scheduledStart() != null && result.scheduledEnd() != null) {
            SessionDraftRequest session = new SessionDraftRequest(
                null,
                result.scheduledStart(),
                result.scheduledEnd(),
                SessionStatus.SCHEDULED
            );
            TaskWithSessionsRequest withSessions = new TaskWithSessionsRequest(
                createRequest, List.of(session)
            );
            return taskService.createTaskWithSessions(userId, withSessions);
        }

        return new TaskWithSessionsResponse(
            taskService.createTask(userId, createRequest),
            List.of()
        );
    }

    /**
     * Parse failures get one retry, same policy as goal decomposition. Unlike that
     * flow there's no sensible partial response to fall back to here, so a second
     * failure surfaces as {@link AiUnavailableException} rather than a degraded result.
     */
    private TaskEnrichmentResult generateResult(List<Message> messages, UUID userId) {
        try {
            return codec.parseResult(callModel(messages, userId));
        } catch (TaskEnrichmentCodec.ResultParseException first) {
            log.warn("Task enrichment reply was not valid JSON, retrying once", first);
            try {
                return codec.parseResult(callModel(messages, userId));
            } catch (TaskEnrichmentCodec.ResultParseException second) {
                throw new AiUnavailableException(second);
            }
        }
    }

    private String callModel(List<Message> messages, UUID userId) {
        try {
            return chatClient.prompt()
                .messages(messages)
                .advisors(a -> a.param("userId", userId.toString()))
                .call()
                .content();
        } catch (Exception e) {
            throw new AiUnavailableException(e);
        }
    }

    /**
     * The user's own description, if they gave one, is never overwritten by the model —
     * only a genuinely blank description gets filled in from the model's inference.
     */
    private String resolveDescription(TaskEnrichmentRequest request, TaskEnrichmentResult result) {
        if (request.description() != null && !request.description().isBlank()) {
            return request.description().strip();
        }
        return result.description() != null ? result.description().strip() : null;
    }

    /** Never trust the model's category id blindly — clear it if it doesn't belong to this user. */
    private CategoryResponse sanitizeCategory(CategoryResponse category, UUID userId) {
        if (category == null || category.id() == null) {
            return null;
        }
        Set<UUID> validIds = categoryRepository.findByUserId(userId).stream()
            .map(Category::getId)
            .collect(Collectors.toSet());
        if (!validIds.contains(category.id())) {
            log.warn("Task enrichment for user {} referenced unknown category id {}; clearing it",
                userId, category.id());
            return null;
        }
        return category;
    }

    private Integer normalizeDuration(Integer minutes) {
        if (minutes == null || minutes < 1) {
            log.warn("Task enrichment returned invalid estimatedDuration {}, defaulting to {}",
                minutes, DEFAULT_ESTIMATED_DURATION_MINUTES);
            return DEFAULT_ESTIMATED_DURATION_MINUTES;
        }
        return minutes;
    }

    private Integer normalizePoints(Integer points) {
        if (points == null || points < 0) {
            return 0;
        }
        return points;
    }
}