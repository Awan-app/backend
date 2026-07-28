package com.ezdo.service.ai.image;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.dto.ai.decompose.DecompositionUserContext;
import com.ezdo.dto.ai.image.ExtractedSession;
import com.ezdo.dto.ai.image.ExtractedTask;
import com.ezdo.dto.ai.image.ImageExtractionResult;
import com.ezdo.dto.ai.image.ImageTaskExtractionResponse;
import com.ezdo.dto.goal.TaskCreateRequest;
import com.ezdo.dto.task.SessionDraftRequest;
import com.ezdo.dto.task.TaskWithSessionsRequest;
import com.ezdo.entity.SessionStatus;
import com.ezdo.exception.AiUnavailableException;
import com.ezdo.exception.InvalidOperationException;
import com.ezdo.exception.ResultParseException;
import com.ezdo.exception.UnsupportedImageTypeException;
import com.ezdo.service.ai.TaskDraftNormalizer;
import com.ezdo.service.ai.UserContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns one uploaded image into a list of proposed tasks, each with zero or more
 * sessions. Nothing is persisted — the result is a proposal the user reviews and
 * then confirms task-by-task against {@code POST /api/v1/tasks/with-sessions}.
 *
 * <p>Two models, two jobs. The vision model reads the image into a faithful text
 * report and deliberately does NOT resolve relative dates; the planning model turns
 * that report into the strict task/session contract, resolving "Fri" and "3pm"
 * against the user's real local clock from USER CONTEXT. Splitting it this way keeps
 * date arithmetic in the model that can actually see the user's timezone, and keeps
 * the strict-JSON contract off the model that is busy reading handwriting.
 */
@Slf4j
@Service
public class ImageTaskExtractionService {

    /** What the vision model replies when the image holds nothing actionable. */
    private static final String NO_TASKS_SENTINEL = "NO TASKS FOUND";

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    /** Content types the vision model accepts, mapped to what Spring AI should send. */
    private static final Map<String, MimeType> SUPPORTED_IMAGE_TYPES = Map.of(
        "image/png", MimeType.valueOf("image/png"),
        "image/jpeg", MimeType.valueOf("image/jpeg"),
        "image/jpg", MimeType.valueOf("image/jpeg"),
        "image/webp", MimeType.valueOf("image/webp"),
        "image/gif", MimeType.valueOf("image/gif")
    );

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final UserContextService userContextService;
    private final TaskDraftNormalizer normalizer;
    private final ChatClient visionClient;
    private final ChatClient planningClient;
    private final ImageTaskPromptBuilder promptBuilder;
    private final ImageTaskExtractionCodec codec;
    private final int maxTasks;
    private final int maxSessionsPerTask;

    public ImageTaskExtractionService(
        UserContextService userContextService,
        TaskDraftNormalizer normalizer,
        @Qualifier("visionModel") ChatClient visionClient,
        @Qualifier("planningModel") ChatClient planningClient,
        ImageTaskPromptBuilder promptBuilder,
        ImageTaskExtractionCodec codec,
        @Value("${ezdo.ai.image.max-tasks}") int maxTasks,
        @Value("${ezdo.ai.image.max-sessions-per-task}") int maxSessionsPerTask
    ) {
        this.userContextService = userContextService;
        this.normalizer = normalizer;
        this.visionClient = visionClient;
        this.planningClient = planningClient;
        this.promptBuilder = promptBuilder;
        this.codec = codec;
        this.maxTasks = maxTasks;
        this.maxSessionsPerTask = maxSessionsPerTask;
    }

    public ImageTaskExtractionResponse extract(UUID userId, MultipartFile image, String note) {
        MimeType mimeType = validateImage(image);
        byte[] bytes = readBytes(image);

        DecompositionUserContext context = userContextService.buildFor(userId);

        String report = callVision(promptBuilder.buildVision(bytes, mimeType, note, context), userId);
        if (isNothingFound(report)) {
            log.info("Image from user {} held no actionable tasks", userId);
            return new ImageTaskExtractionResponse(strip(report), List.of(), Instant.now());
        }

        ImageExtractionResult result =
            generateResult(promptBuilder.buildPlanning(report, context), userId);

        return new ImageTaskExtractionResponse(
            strip(report),
            toDrafts(result, userId),
            Instant.now()
        );
    }

    // --- upload validation ---------------------------------------------------

    private MimeType validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidOperationException("An image file is required");
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new InvalidOperationException(
                "Image is too large; the maximum size is " + (MAX_IMAGE_BYTES / (1024 * 1024)) + "MB");
        }
        String contentType = image.getContentType();
        MimeType mimeType = contentType == null
            ? null
            : SUPPORTED_IMAGE_TYPES.get(contentType.toLowerCase().strip());
        if (mimeType == null) {
            throw new UnsupportedImageTypeException(
                contentType, String.join(", ", SUPPORTED_IMAGE_TYPES.keySet()));
        }
        return mimeType;
    }

    private byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new InvalidOperationException("Could not read the uploaded image");
        }
    }

    // --- model calls ---------------------------------------------------------

    private String callVision(List<Message> messages, UUID userId) {
        try {
            return visionClient.prompt()
                .messages(messages)
                .advisors(a -> a.param("userId", userId.toString()))
                .call()
                .content();
        } catch (Exception e) {
            throw new AiUnavailableException(e);
        }
    }

    /**
     * Parse failures get one retry, the same policy as the other AI flows. A second
     * failure has no sensible partial answer to degrade to, so it surfaces as
     * {@link AiUnavailableException}.
     */
    private ImageExtractionResult generateResult(List<Message> messages, UUID userId) {
        try {
            return codec.parseResult(callPlanning(messages, userId));
        } catch (ResultParseException first) {
            log.warn("Image task planning reply was not valid JSON, retrying once", first);
            try {
                return codec.parseResult(callPlanning(messages, userId));
            } catch (ResultParseException second) {
                throw new AiUnavailableException(second);
            }
        }
    }

    private String callPlanning(List<Message> messages, UUID userId) {
        try {
            return planningClient.prompt()
                .messages(messages)
                .advisors(a -> a.param("userId", userId.toString()))
                .call()
                .content();
        } catch (Exception e) {
            throw new AiUnavailableException(e);
        }
    }

    private boolean isNothingFound(String report) {
        return report == null
            || report.isBlank()
            || report.strip().toUpperCase().startsWith(NO_TASKS_SENTINEL);
    }

    // --- draft mapping -------------------------------------------------------

    /**
     * Every field here comes from a model, so every field is re-checked: caps are
     * enforced server-side rather than trusted to the prompt, category ids are
     * matched against what the user really owns, and impossible sessions are dropped
     * instead of being handed to the client as a request it cannot post back.
     */
    private List<TaskWithSessionsRequest> toDrafts(ImageExtractionResult result, UUID userId) {
        List<ExtractedTask> tasks = result.tasks() != null ? result.tasks() : List.of();
        if (tasks.size() > maxTasks) {
            log.warn("Image extraction for user {} returned {} tasks; keeping the first {}",
                userId, tasks.size(), maxTasks);
            tasks = tasks.subList(0, maxTasks);
        }

        Set<UUID> validCategoryIds = normalizer.validCategoryIds(userId);
        List<TaskWithSessionsRequest> drafts = new ArrayList<>();

        for (ExtractedTask task : tasks) {
            String title = normalizer.truncate(task.title(), MAX_TITLE_LENGTH);
            if (title == null) {
                log.warn("Image extraction for user {} produced a task with no title; dropping it", userId);
                continue;
            }

            CategoryResponse category =
                normalizer.sanitizeCategory(task.category(), validCategoryIds, userId);

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

            drafts.add(new TaskWithSessionsRequest(createRequest, toSessions(task, userId)));
        }
        return drafts;
    }

    private List<SessionDraftRequest> toSessions(ExtractedTask task, UUID userId) {
        if (task.sessions() == null || task.sessions().isEmpty()) {
            return List.of();
        }
        List<SessionDraftRequest> sessions = new ArrayList<>();
        for (ExtractedSession session : task.sessions()) {
            if (session == null || session.start() == null || session.end() == null
                || !session.end().isAfter(session.start())) {
                log.warn("Image extraction for user {} produced an invalid session for task '{}'; dropping it",
                    userId, task.title());
                continue;
            }
            if (sessions.size() == maxSessionsPerTask) {
                log.warn("Image extraction for user {} produced more than {} sessions for task '{}'; truncating",
                    userId, maxSessionsPerTask, task.title());
                break;
            }
            sessions.add(new SessionDraftRequest(
                null, session.start(), session.end(), SessionStatus.SCHEDULED));
        }
        return sessions;
    }

    private String strip(String value) {
        return value == null ? null : value.strip();
    }
}
