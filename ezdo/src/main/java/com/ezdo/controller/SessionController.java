package com.ezdo.controller;

import com.ezdo.dto.SessionRequest;
import com.ezdo.dto.SessionResponse;
import com.ezdo.entity.SessionStatus;
import com.ezdo.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/zones/{zoneId}")
    public ResponseEntity<SessionResponse> create(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID zoneId,
            @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(userId, zoneId, request));
    }

    @GetMapping("/zones/{zoneId}")
    public ResponseEntity<List<SessionResponse>> getByZone(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(sessionService.getByZone(zoneId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> get(@AuthenticationPrincipal UUID userId, @PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getById(userId, sessionId));
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.ok(sessionService.update(userId, sessionId, request));
    }

    @PatchMapping("/{sessionId}/status")
    public ResponseEntity<SessionResponse> updateStatus(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID sessionId,
            @RequestParam SessionStatus status) {
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
