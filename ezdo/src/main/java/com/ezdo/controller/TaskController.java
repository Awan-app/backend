package com.ezdo.controller;

import com.ezdo.dto.goal.TaskCreateRequest;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.dto.goal.TaskUpdateRequest;
import com.ezdo.dto.task.TaskDependencyRequest;
import com.ezdo.dto.task.TaskMoveRequest;
import com.ezdo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskInfoResponse> create(@AuthenticationPrincipal UUID userId,
                                                   @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskService.createTask(userId, request));
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
