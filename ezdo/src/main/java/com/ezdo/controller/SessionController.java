package com.ezdo.controller;

import com.ezdo.dto.SessionRequest;
import com.ezdo.dto.SessionResponse;
import com.ezdo.entity.SessionStatus;
import com.ezdo.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/zones/{zoneId}")
    public ResponseEntity<SessionResponse> create(
            @PathVariable UUID zoneId,
            @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(zoneId, request));
    }

    @GetMapping("/zones/{zoneId}")
    public ResponseEntity<List<SessionResponse>> getByZone(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(sessionService.getByZone(zoneId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> get(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getById(sessionId));
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> update(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.ok(sessionService.update(sessionId, request));
    }

    @PatchMapping("/{sessionId}/status")
    public ResponseEntity<SessionResponse> updateStatus(
            @PathVariable UUID sessionId,
            @RequestParam SessionStatus status) {
        return ResponseEntity.ok(sessionService.updateStatus(sessionId, status));
    }

    @PatchMapping("/{sessionId}/lock")
    public ResponseEntity<SessionResponse> lock(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.lock(sessionId));
    }

    @PatchMapping("/{sessionId}/unlock")
    public ResponseEntity<SessionResponse> unlock(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.unlock(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID sessionId) {
        sessionService.delete(sessionId);
        return ResponseEntity.noContent().build();
    }

}
