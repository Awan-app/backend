package com.ezdo.controller;

import com.ezdo.dto.SessionResponse;
import com.ezdo.dto.ai.schedule.GoalScheduleRequest;
import com.ezdo.dto.ai.schedule.GoalScheduleResponse;
import com.ezdo.dto.ai.schedule.TaskScheduleRequest;
import com.ezdo.dto.ai.schedule.TaskScheduleResponse;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.service.GoalSchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedule")
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

    // for testing only
    @PostMapping("/preview")
    public ResponseEntity<List<SessionResponse>> previewSchedule(
            @AuthenticationPrincipal UUID userId,
            @RequestBody List<TaskInfoResponse> tasks,
            @RequestParam(required = false) Integer horizonDays
    ) {
        return ResponseEntity.ok(schedulingService.previewSchedule(userId, tasks, horizonDays));
    }

}
