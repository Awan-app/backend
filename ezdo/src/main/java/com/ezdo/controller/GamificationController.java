package com.ezdo.controller;

import com.ezdo.dto.profile.UserProgressResponse;
import com.ezdo.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
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

    @GetMapping("/activity-dates")
    public ResponseEntity<List<LocalDate>> getActivityDates(
        @AuthenticationPrincipal UUID userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(gamificationService.getActivityDates(userId, startDate, endDate));
    }
}
