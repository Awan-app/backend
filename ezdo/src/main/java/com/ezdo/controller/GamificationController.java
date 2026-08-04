package com.ezdo.controller;

import com.ezdo.dto.profile.UserProgressResponse;
import com.ezdo.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/progress")
    public ResponseEntity<UserProgressResponse> getProgress(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(gamificationService.getProgress(userId));
    }
}
