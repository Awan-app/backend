package com.ezdo.controller;

import com.ezdo.dto.ai.GoalScheduleRequest;
import com.ezdo.dto.ai.GoalScheduleResponse;
import com.ezdo.dto.ai.TaskScheduleRequest;
import com.ezdo.dto.ai.TaskScheduleResponse;
import com.ezdo.service.GoalSchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/schedule")
@RequiredArgsConstructor
public class SchedulingController {

    private final GoalSchedulingService schedulingService;

    @PostMapping
    public ResponseEntity<GoalScheduleResponse> scheduleGoal(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody GoalScheduleRequest request
    ) {
        return ResponseEntity.ok(schedulingService.scheduleGoal(userId, request));
    }

    @PostMapping("/task")
    public ResponseEntity<TaskScheduleResponse> scheduleTask(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody TaskScheduleRequest request
    ) {
        return ResponseEntity.ok(schedulingService.scheduleTask(userId, request));
    }

}
