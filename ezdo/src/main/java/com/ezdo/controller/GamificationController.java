package com.ezdo.controller;

import com.ezdo.dto.gamification.WheelConfigResponse;
import com.ezdo.dto.gamification.WheelSpinResponse;
import com.ezdo.dto.profile.UserProgressResponse;
import com.ezdo.service.DailyGiftService;
import com.ezdo.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final DailyGiftService dailyGiftService;

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

    @GetMapping("/last-activity-date")
    public ResponseEntity<Map<String, LocalDate>> getLastActivityDate(@AuthenticationPrincipal UUID userId) {
        LocalDate date = gamificationService.getLastActivityDate(userId);
        return ResponseEntity.ok(Map.of("lastActivityDate", date));
    }

    @GetMapping("/wheel/config")
    public ResponseEntity<WheelConfigResponse> getDailyGiftConfig(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(dailyGiftService.getConfig(userId));
    }

    @PostMapping("/wheel/spin")
    public ResponseEntity<WheelSpinResponse> spinDailyGift(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(dailyGiftService.spin(userId));
    }
}
