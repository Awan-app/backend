package com.ezdo.controller;

import com.ezdo.dto.goal.GoalCreateRequest;
import com.ezdo.dto.goal.GoalInfoResponse;
import com.ezdo.dto.goal.GoalUpdateRequest;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.entity.GoalStatus;
import com.ezdo.service.GoalService;
import com.ezdo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<GoalInfoResponse> create(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody GoalCreateRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(goalService.createGoal(userId, req));
    }

    @GetMapping
    public Page<GoalInfoResponse> list(
        @AuthenticationPrincipal UUID userId,
        @RequestParam(required = false) GoalStatus status,
        @RequestParam(defaultValue = "false") boolean includeInbox,
        Pageable pageable
    ) {
        return goalService.listGoals(userId, status, includeInbox, pageable);
    }

    @GetMapping("/inbox")
    public GoalInfoResponse inbox(
        @AuthenticationPrincipal UUID userId
    ) {
        return goalService.getOrCreateInboxResponse(userId);
    }

    @GetMapping("/{goalId}")
    public GoalInfoResponse get(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID goalId,
        @RequestParam(defaultValue = "false") boolean expand
    ) {
        return goalService.getGoal(userId, goalId, expand);
    }

    @GetMapping("/{goalId}/tasks")
    public List<TaskInfoResponse> listForGoal(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID goalId
    ) {
        return taskService.listTasksForGoal(userId, goalId);
    }

    @PatchMapping("/{goalId}")
    public GoalInfoResponse update(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID goalId,
        @Valid @RequestBody GoalUpdateRequest req
    ) {
        return goalService.updateGoal(userId, goalId, req);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID goalId
    ) {
        goalService.deleteGoal(userId, goalId);
        return ResponseEntity.noContent().build();
    }
}
