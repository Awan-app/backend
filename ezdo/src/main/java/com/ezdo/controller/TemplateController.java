package com.ezdo.controller;

import com.ezdo.dto.CreateTemplateRequest;
import com.ezdo.dto.TemplateResponse;
import com.ezdo.dto.UpdateTemplateRequest;
import com.ezdo.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/template")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<TemplateResponse> create(
            @AuthenticationPrincipal UUID userId ,
            @Valid @RequestBody CreateTemplateRequest createTemplateRequest){
        return ResponseEntity.ok(templateService.createTemplate(userId,createTemplateRequest));

    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAll(@AuthenticationPrincipal UUID userId){
        return ResponseEntity.ok(templateService.getTemplatesByUser(userId));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<TemplateResponse>get(@PathVariable UUID templateId){
        return ResponseEntity.ok(templateService.getTemplateById(templateId));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<TemplateResponse>update( @AuthenticationPrincipal UUID userId, @PathVariable UUID templateId ,
                                                  @Valid @RequestBody UpdateTemplateRequest updateTemplateRequest){
        return ResponseEntity.ok(templateService.updateTemplate( userId , templateId , updateTemplateRequest));

    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void>delete(@PathVariable UUID templateId){
        templateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();

    }
}
