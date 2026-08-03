package com.ezdo.controller;

import com.ezdo.dto.SessionResponse;
import com.ezdo.dto.ZoneRequest;
import com.ezdo.dto.ZoneResponse;
import com.ezdo.entity.SessionStatus;
import com.ezdo.service.SessionService;
import com.ezdo.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/zones")
public class ZoneController {

    private final ZoneService zoneService;
    private final SessionService sessionService;

    @GetMapping("/{zoneId}")
    public ResponseEntity<ZoneResponse> get(@AuthenticationPrincipal UUID userId, @PathVariable UUID zoneId) {
        return ResponseEntity.ok(zoneService.getById(userId, zoneId));
    }

    @GetMapping("/{zoneId}/sessions")
    public ResponseEntity<List<SessionResponse>> getByZone(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID zoneId,
        @RequestParam(required = false) SessionStatus status
    ) {
        return ResponseEntity.ok(sessionService.getByZone(zoneId, userId, status));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<ZoneResponse>> getZonesByDate(
        @AuthenticationPrincipal UUID userId,
        @PathVariable
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
    ) {
        return ResponseEntity.ok(zoneService.getZonesByDate(userId, date));
    }

    @PutMapping("/{zoneId}")
    public ResponseEntity<ZoneResponse> update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID zoneId,
            @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.update(userId, zoneId, request));
    }

    @DeleteMapping("/{zoneId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID zoneId) {
        zoneService.delete(userId, zoneId);
        return ResponseEntity.noContent().build();
    }
}
