package com.ezdo.controller;

import com.ezdo.dto.ai.decompose.ChatMessage;
import com.ezdo.dto.ai.decompose.ChatReply;
import com.ezdo.dto.ai.decompose.TranscriptResponse;
import com.ezdo.dto.ai.enrich.TaskEnrichmentRequest;
import com.ezdo.dto.goal.GoalInfoResponse;
import com.ezdo.service.ai.decompose.GoalDecompositionService;
import com.ezdo.service.ai.enrich.TaskEnrichmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class DecompositionController {

    private final GoalDecompositionService decompositionService;
    private final TaskEnrichmentService enrichmentService;

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
