package com.ezdo.controller;

import com.ezdo.dto.*;
import com.ezdo.service.TemplateService;
import com.ezdo.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final ZoneService zoneService;

    @PostMapping
    public ResponseEntity<TemplateResponse> create(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreateTemplateRequest createTemplateRequest
    ) {
        return ResponseEntity.ok(templateService.createTemplate(userId, createTemplateRequest));
    }

    @PostMapping("/{templateId}/zones")
    public ResponseEntity<ZoneResponse> addToTemplate(@AuthenticationPrincipal UUID userId,
                                                      @PathVariable UUID templateId,
                                                      @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.addZoneToTemplate(userId, templateId, request));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAll(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(templateService.getTemplatesByUser(userId));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> get(@AuthenticationPrincipal UUID userId, @PathVariable UUID templateId) {
        return ResponseEntity.ok(templateService.getTemplateById(userId, templateId));
    }

    @GetMapping("/{templateId}/zones")
    public ResponseEntity<List<ZoneResponse>> getByTemplate(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(zoneService.getByTemplate(userId, templateId));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<TemplateResponse> update(@AuthenticationPrincipal UUID userId,
                                                   @PathVariable UUID templateId,
                                                   @Valid @RequestBody UpdateTemplateRequest updateTemplateRequest) {
        return ResponseEntity.ok(templateService.updateTemplate(userId, templateId, updateTemplateRequest));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID templateId) {
        templateService.deleteTemplate(userId, templateId);
        return ResponseEntity.noContent().build();
    }
}
