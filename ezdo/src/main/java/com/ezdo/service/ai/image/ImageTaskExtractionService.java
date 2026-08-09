package com.ezdo.service.ai.image;

import com.ezdo.dto.ai.AiUserPreferencesContext;
import com.ezdo.dto.ai.plan.TaskProposalResponse;
import com.ezdo.exception.AiUnavailableException;
import com.ezdo.exception.InvalidOperationException;
import com.ezdo.exception.UnsupportedImageTypeException;
import com.ezdo.service.ai.UserContextService;
import com.ezdo.service.ai.plan.SourceKind;
import com.ezdo.service.ai.plan.TaskPlanningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The vision half of image-to-tasks: validates the upload, reads the image into a
 * faithful text report, and hands that report to {@link TaskPlanningService} — the
 * same planner that serves notes the user types.
 *
 * <p>Two models, two jobs. The vision model deliberately does NOT resolve relative
 * dates; it reports "Fri" and "3pm" as written, and the planning model resolves them
 * against the user's real local clock from USER CONTEXT. Splitting it this way keeps
 * date arithmetic in the model that can see the user's timezone, and keeps the
 * strict-JSON contract off the model that is busy reading handwriting.
 *
 * <p>Nothing is persisted — the result is a proposal the user confirms task-by-task
 * against {@code POST /api/v1/tasks/with-sessions}.
 */
@Slf4j
@Service
public class ImageTaskExtractionService {

    /** What the vision model replies when the image holds nothing actionable. */
    private static final String NO_TASKS_SENTINEL = "NO TASKS FOUND";

    /** Content types the vision model accepts, mapped to what Spring AI should send. */
    private static final Map<String, MimeType> SUPPORTED_IMAGE_TYPES = Map.of(
        "image/png",    MimeType.valueOf("image/png"),
        "image/jpeg",   MimeType.valueOf("image/jpeg"),
        "image/jpg",    MimeType.valueOf("image/jpeg"),
        "image/webp",   MimeType.valueOf("image/webp"),
        "image/gif",    MimeType.valueOf("image/gif")
    );

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final UserContextService userContextService;
    private final TaskPlanningService planningService;
    private final ChatClient visionClient;
    private final ImageTaskPromptBuilder promptBuilder;

    public ImageTaskExtractionService(
        UserContextService userContextService,
        TaskPlanningService planningService,
        @Qualifier("visionModel") ChatClient visionClient,
        ImageTaskPromptBuilder promptBuilder
    ) {
        this.userContextService = userContextService;
        this.planningService = planningService;
        this.visionClient = visionClient;
        this.promptBuilder = promptBuilder;
    }

    public TaskProposalResponse extract(UUID userId, MultipartFile image, String note) {
        MimeType mimeType = validateImage(image);
        byte[] bytes = readBytes(image);

        AiUserPreferencesContext context = userContextService.buildFor(userId);

        String report = callVision(promptBuilder.buildVision(bytes, mimeType, note, context), userId);
        if (isNothingFound(report)) {
            log.info("Image from user {} held no actionable tasks", userId);
            return new TaskProposalResponse(strip(report), List.of(), Instant.now());
        }

        // log.info("Image from user {} held actionable tasks: {}", userId, report);

        return new TaskProposalResponse(
            strip(report),
            planningService.plan(userId, report, SourceKind.IMAGE_REPORT, context),
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

    // --- vision call ---------------------------------------------------------

    private String callVision(List<Message> messages, UUID userId) {
        try {
            return visionClient.prompt()
                .messages(messages)
                .advisors(a -> a.param("userId", userId.toString()))
                .call()
                .content();
        } catch (Exception e) {
            log.error("", e);
            throw new AiUnavailableException(e);
        }
    }

    private boolean isNothingFound(String report) {
        return report == null
            || report.isBlank()
            || report.strip().toUpperCase().startsWith(NO_TASKS_SENTINEL);
    }

    private String strip(String value) {
        return value == null ? null : value.strip();
    }
}
