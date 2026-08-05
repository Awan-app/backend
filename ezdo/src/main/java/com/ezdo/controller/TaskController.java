package com.ezdo.controller;

import com.ezdo.dto.goal.TaskCreateRequest;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.dto.goal.TaskUpdateRequest;
import com.ezdo.dto.task.TaskDependencyRequest;
import com.ezdo.dto.task.TaskMoveRequest;
import com.ezdo.dto.SessionResponse;
import com.ezdo.dto.task.AddSessionsRequest;
import com.ezdo.dto.task.TasksWithSessionsRequest;
import com.ezdo.dto.task.TasksWithSessionsResponse;
import com.ezdo.dto.task.TaskWithSessionsRequest;
import com.ezdo.dto.task.TaskWithSessionsResponse;
import com.ezdo.entity.SessionStatus;
import com.ezdo.service.SessionService;
import com.ezdo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final SessionService sessionService;

    @GetMapping("/date/{date}")
    public ResponseEntity<List<TaskWithSessionsResponse>> getByDate(
        @AuthenticationPrincipal UUID userId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(taskService.getTasksByDate(userId, date));
    }

    @GetMapping("/range")
    public ResponseEntity<Map<LocalDate, List<TaskWithSessionsResponse>>> getByDateRange(
        @AuthenticationPrincipal UUID userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(taskService.getTasksByDateRange(userId, startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<TaskInfoResponse> create(@AuthenticationPrincipal UUID userId,
                                                   @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskService.createTask(userId, request));
    }

    @PostMapping("/with-sessions")
    public ResponseEntity<TaskWithSessionsResponse> createWithSessions(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody TaskWithSessionsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskService.createTaskWithSessions(userId, request));
    }

    @PostMapping("/with-sessions/bulk")
    public ResponseEntity<TasksWithSessionsResponse> createTasksWithSessions(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody TasksWithSessionsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskService.createTasksWithSessionsBulk(userId, request));
    }

    @GetMapping("/{taskId}/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID taskId,
        @RequestParam(required = false) SessionStatus status
    ) {
        return ResponseEntity.ok(sessionService.getByTask(taskId, userId, status));
    }

    @PostMapping("/{taskId}/sessions")
    public ResponseEntity<List<SessionResponse>> addSessions(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID taskId,
        @Valid @RequestBody AddSessionsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskService.addSessionsToTask(userId, taskId, request));
    }

    @GetMapping("/{taskId}")
    public TaskInfoResponse get(@AuthenticationPrincipal UUID userId,
                                @PathVariable UUID taskId) {
        return taskService.getTask(userId, taskId);
    }

    @PatchMapping("/{taskId}")
    public TaskInfoResponse update(@AuthenticationPrincipal UUID userId,
                                   @PathVariable UUID taskId,
                                   @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.updateTask(userId, taskId, request);
    }

    @PatchMapping("/{taskId}/move")
    public TaskInfoResponse move(@AuthenticationPrincipal UUID userId,
                                 @PathVariable UUID taskId,
                                 @Valid @RequestBody TaskMoveRequest request) {
        return taskService.moveTask(userId, taskId, request);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId,
                                       @PathVariable UUID taskId,
                                       @RequestParam(defaultValue = "false") boolean cascade) {
        taskService.deleteTask(userId, taskId, cascade);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/dependencies")
    public ResponseEntity<Void> addDependency(@AuthenticationPrincipal UUID userId,
                                              @PathVariable UUID taskId,
                                              @Valid @RequestBody TaskDependencyRequest request) {
        taskService.addDependency(userId, taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{taskId}/dependencies/{dependsOnTaskId}")
    public ResponseEntity<Void> removeDependency(@AuthenticationPrincipal UUID userId,
                                                 @PathVariable UUID taskId,
                                                 @PathVariable UUID dependsOnTaskId) {
        taskService.removeDependency(userId, taskId, dependsOnTaskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{taskId}/dependencies")
    public List<TaskInfoResponse> dependencies(@AuthenticationPrincipal UUID userId,
                                               @PathVariable UUID taskId) {
        return taskService.listDependencies(userId, taskId);
    }

    @GetMapping("/{taskId}/dependents")
    public List<TaskInfoResponse> dependents(@AuthenticationPrincipal UUID userId,
                                             @PathVariable UUID taskId) {
        return taskService.listDependents(userId, taskId);
    }
}
