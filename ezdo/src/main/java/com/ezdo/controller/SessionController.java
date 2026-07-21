package com.ezdo.controller;

import com.ezdo.dto.SessionRequest;
import com.ezdo.dto.SessionResponse;
import com.ezdo.entity.SessionStatus;
import com.ezdo.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> get(@AuthenticationPrincipal UUID userId, @PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getById(userId, sessionId));
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> update(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID sessionId,
        @Valid @RequestBody SessionRequest request
    ) {
        return ResponseEntity.ok(sessionService.update(userId, sessionId, request));
    }

    @PatchMapping("/{sessionId}/status")
    public ResponseEntity<SessionResponse> updateStatus(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID sessionId,
        @RequestParam SessionStatus status
    ) {
        return ResponseEntity.ok(sessionService.updateStatus(userId, sessionId, status));
    }

    @PatchMapping("/{sessionId}/lock")
    public ResponseEntity<SessionResponse> lock(@AuthenticationPrincipal UUID userId, @PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.lock(userId, sessionId));
    }

    @PatchMapping("/{sessionId}/unlock")
    public ResponseEntity<SessionResponse> unlock(@AuthenticationPrincipal UUID userId, @PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.unlock(userId, sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID sessionId) {
        sessionService.delete(userId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
