package com.ezdo.controller;

import com.ezdo.dto.ZoneRequest;
import com.ezdo.dto.ZoneResponse;
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

    @PostMapping("/template/{templateId}")
    public ResponseEntity<ZoneResponse> addToTemplate(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID templateId,
            @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.addZoneToTemplate(userId, templateId, request));
    }

    @PostMapping("/override/{overrideId}")
    public ResponseEntity<ZoneResponse> addToOverride(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID overrideId,
            @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.addZoneToOverride(userId, overrideId, request));
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<ZoneResponse>> getByTemplate(@PathVariable UUID templateId) {
        return ResponseEntity.ok(zoneService.getByTemplate(templateId));
    }

    @GetMapping("/override/{overrideId}")
    public ResponseEntity<List<ZoneResponse>> getByOverride(@PathVariable UUID overrideId) {
        return ResponseEntity.ok(zoneService.getByOverride(overrideId));
    }

    @GetMapping("/{zoneId}")
    public ResponseEntity<ZoneResponse> get(@AuthenticationPrincipal UUID userId, @PathVariable UUID zoneId) {
        return ResponseEntity.ok(zoneService.getById(userId, zoneId));
    }

    @GetMapping("/date")
    public ResponseEntity<List<ZoneResponse>> getZonesByDate(
            @AuthenticationPrincipal UUID userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

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
