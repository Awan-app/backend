package com.ezdo.service.ai.enrich;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.dto.ai.decompose.DecompositionUserContext;
import com.ezdo.dto.ai.enrich.TaskEnrichmentRequest;
import com.ezdo.dto.ai.enrich.TaskEnrichmentResult;
import com.ezdo.dto.goal.TaskCreateRequest;
import com.ezdo.dto.task.SessionDraftRequest;
import com.ezdo.dto.task.TaskWithSessionsRequest;
import com.ezdo.dto.task.TaskWithSessionsResponse;
import com.ezdo.entity.SessionStatus;
import com.ezdo.exception.AiUnavailableException;
import com.ezdo.exception.InvalidOperationException;
import com.ezdo.service.TaskService;
import com.ezdo.service.ai.TaskDraftNormalizer;
import com.ezdo.service.ai.UserContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Single-shot enrichment: given a task title/description, asks the model to fill in
 * the operational fields (description if blank, duration, points, mandatory,
 * splitting, category, and optionally scheduling info) grounded in the user's real
 * categories and scheduling preferences. Creates the Task and, if the user mentioned
 * timing, also creates Sessions.
 */
@Slf4j
@Service
public class TaskEnrichmentService {

    private final UserContextService userContextService;
    private final TaskDraftNormalizer normalizer;
    private final ChatClient chatClient;
    private final TaskEnrichmentPromptBuilder promptBuilder;
    private final TaskEnrichmentCodec codec;
    private final TaskService taskService;

    public TaskEnrichmentService(
        UserContextService userContextService,
        TaskDraftNormalizer normalizer,
        @Qualifier("planningModel") ChatClient chatClient,
        TaskEnrichmentPromptBuilder promptBuilder,
        TaskEnrichmentCodec codec,
        TaskService taskService
    ) {
        this.userContextService = userContextService;
        this.normalizer = normalizer;
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.codec = codec;
        this.taskService = taskService;
    }

    public TaskWithSessionsResponse enrich(UUID userId, TaskEnrichmentRequest request) {
        TaskWithSessionsRequest draft = enrichNoPersist(userId, request);
        if (draft.sessions().isEmpty()) {
            return new TaskWithSessionsResponse(
                taskService.createTask(userId, draft.task()),
                List.of()
            );
        }
        return taskService.createTaskWithSessions(userId, draft);
    }

    public TaskWithSessionsRequest enrichNoPersist(UUID userId, TaskEnrichmentRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new InvalidOperationException("Task title is required");
        }

        DecompositionUserContext context = userContextService.buildFor(userId);
        List<Message> messages = promptBuilder.build(request, context);

        TaskEnrichmentResult result = generateResult(messages, userId);
        CategoryResponse category = normalizer.sanitizeCategory(
            result.category(), normalizer.validCategoryIds(userId), userId);

        TaskCreateRequest createRequest = new TaskCreateRequest(
            request.title().strip(),
            resolveDescription(request, result),
            normalizer.normalizeDuration(result.estimatedDuration()),
            result.mandatory() == null || result.mandatory(),
            normalizer.normalizePoints(result.estimatedPoints()),
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
            return new TaskWithSessionsRequest(createRequest, List.of(session));
        }

        return new TaskWithSessionsRequest(createRequest, List.of());
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
}
