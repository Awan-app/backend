package com.ezdo.controller;

import com.ezdo.dto.ai.decompose.ChatMessage;
import com.ezdo.dto.ai.decompose.ChatReply;
import com.ezdo.dto.ai.decompose.TranscriptResponse;
import com.ezdo.dto.ai.enrich.TaskEnrichmentRequest;
import com.ezdo.dto.ai.image.ImageTaskExtractionResponse;
import com.ezdo.dto.goal.GoalInfoResponse;
import com.ezdo.service.ai.decompose.GoalDecompositionService;
import com.ezdo.service.ai.enrich.TaskEnrichmentService;
import com.ezdo.service.ai.image.ImageTaskExtractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final GoalDecompositionService decompositionService;
    private final TaskEnrichmentService enrichmentService;
    private final ImageTaskExtractionService imageTaskExtractionService;

    @PostMapping("/goal-decompose")
    public ChatReply sendMessage(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChatMessage request
    ) {
        return decompositionService.processMessage(userId, request);
    }

    @PostMapping("/goal-decompose/{sessionId}/confirm")
    public ResponseEntity<GoalInfoResponse> confirm(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId
    ) {
        GoalInfoResponse result = decompositionService.confirmDecomposition(userId, sessionId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/task-create")
    public ResponseEntity<?> create(
        @AuthenticationPrincipal UUID userId,
        @RequestParam(defaultValue = "true") Boolean persist,
        @Valid @RequestBody TaskEnrichmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(persist ? enrichmentService.enrich(userId, request) : enrichmentService.enrichNoPersist(userId, request));
    }

    @PostMapping(value = "/image-to-tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageTaskExtractionResponse extract(
        @AuthenticationPrincipal UUID userId,
        @RequestPart("image") MultipartFile image,
        @RequestPart(value = "note", required = false) String note
    ) {
        return imageTaskExtractionService.extract(userId, image, note);
    }

    @GetMapping("/goal-decompose/{sessionId}")
    public TranscriptResponse getTranscript(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId
    ) {
        return decompositionService.getTranscript(userId, sessionId);
    }

    @PostMapping("/goal-decompose/{sessionId}/cancel")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId
    ) {
        decompositionService.cancel(userId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
