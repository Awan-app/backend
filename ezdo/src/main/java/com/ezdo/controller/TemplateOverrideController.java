package com.ezdo.controller;

import com.ezdo.dto.*;
import com.ezdo.service.TemplateOverrideService;
import com.ezdo.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/template-overrides")
@RequiredArgsConstructor
public class TemplateOverrideController {

    private final TemplateOverrideService templateOverrideService;
    private final ZoneService zoneService;

    @PostMapping
    public ResponseEntity<TemplateOverrideResponse> create(@AuthenticationPrincipal UUID userId,
                                                           @Valid @RequestBody TemplateOverrideRequest request) {
        return ResponseEntity.ok(templateOverrideService.create(userId, request));
    }

    @PostMapping("/{overrideId}/zones")
    public ResponseEntity<ZoneResponse> addToOverride(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID overrideId,
        @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.addZoneToOverride(userId, overrideId, request));
    }

    @GetMapping
    public ResponseEntity<List<TemplateOverrideResponse>> getAll(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(templateOverrideService.getByUser(userId));
    }

    @GetMapping("/{overrideId}")
    public ResponseEntity<TemplateOverrideResponse> get(@AuthenticationPrincipal UUID userId, @PathVariable UUID overrideId) {
        return ResponseEntity.ok(templateOverrideService.getById(userId, overrideId));
    }

    @GetMapping("/{overrideId}/zones")
    public ResponseEntity<List<ZoneResponse>> getByOverride(@AuthenticationPrincipal UUID userId,
                                                            @PathVariable UUID overrideId) {
        return ResponseEntity.ok(zoneService.getByTemplateOverride(userId, overrideId));
    }

    @PutMapping("/{overrideId}")
    public ResponseEntity<TemplateOverrideResponse> update(@AuthenticationPrincipal UUID userId,
                                                           @PathVariable UUID overrideId,
                                                           @Valid @RequestBody TemplateOverrideRequest request) {
        return ResponseEntity.ok(templateOverrideService.updateTemplate(userId, overrideId, request));
    }

    @PutMapping("/{templateOverrideId}/zones")
    public ResponseEntity<List<ZoneResponse>> updateZonesTemplate(@AuthenticationPrincipal UUID userId ,
                                                                  @PathVariable UUID templateOverrideId ,
                                                                  @Valid @RequestBody UpdateTemplateZoneRequest request){
        return ResponseEntity.ok(templateOverrideService.updateTemplateZones(templateOverrideId , userId , request));
    }

    @DeleteMapping("/{overrideId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID overrideId) {
        templateOverrideService.delete(userId, overrideId);
        return ResponseEntity.noContent().build();
    }
}
