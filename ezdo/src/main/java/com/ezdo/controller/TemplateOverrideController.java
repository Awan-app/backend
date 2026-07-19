package com.ezdo.controller;

import com.ezdo.dto.TemplateOverrideRequest;
import com.ezdo.dto.TemplateOverrideResponse;
import com.ezdo.service.TemplateOverrideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<TemplateOverrideResponse> create(@AuthenticationPrincipal UUID userId,
                                                           @Valid @RequestBody TemplateOverrideRequest request) {
        return ResponseEntity.ok(templateOverrideService.create(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<TemplateOverrideResponse>> getAll(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(templateOverrideService.getByUser(userId));
    }

    @GetMapping("/{overrideId}")
    public ResponseEntity<TemplateOverrideResponse> get(@AuthenticationPrincipal UUID userId, @PathVariable UUID overrideId) {
        return ResponseEntity.ok(templateOverrideService.getById(userId, overrideId));
    }

    @PutMapping("/{overrideId}")
    public ResponseEntity<TemplateOverrideResponse> update(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID overrideId,
        @Valid @RequestBody TemplateOverrideRequest request) {
        return ResponseEntity.ok(templateOverrideService.updateTemplate(userId, overrideId, request));
    }

    @DeleteMapping("/{overrideId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID overrideId) {
        templateOverrideService.delete(userId, overrideId);
        return ResponseEntity.noContent().build();
    }


}
