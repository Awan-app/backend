package com.ezdo.controller;


import com.ezdo.dto.ai.schedule.GoalScheduleRequest;
import com.ezdo.dto.ai.schedule.GoalScheduleResponse;
import com.ezdo.service.AISchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/schedule")
@RequiredArgsConstructor
public class AISchedulingController {

    private final AISchedulingService schedulingService;

    @PostMapping
    public ResponseEntity<GoalScheduleResponse> scheduleGoal(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody GoalScheduleRequest request
    ) {
        return ResponseEntity.ok(schedulingService.scheduleGoal(userId, request));
    }
}