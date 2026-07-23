package com.ezdo.service;

import com.ezdo.dto.goal.TaskCreateRequest;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.dto.goal.TaskUpdateRequest;
import com.ezdo.dto.task.TaskDependencyRequest;
import com.ezdo.dto.task.TaskMoveRequest;
import com.ezdo.dto.task.TaskWithSessionsRequest;
import com.ezdo.dto.task.TaskWithSessionsResponse;
import com.ezdo.entity.Goal;
import com.ezdo.entity.Session;
import com.ezdo.entity.SessionStatus;
import com.ezdo.entity.Task;
import com.ezdo.entity.TaskStatus;
import com.ezdo.entity.User;
import com.ezdo.entity.Zone;
import com.ezdo.exception.*;
import com.ezdo.mapper.SessionMapper;
import com.ezdo.mapper.TaskMapper;
import com.ezdo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final GoalService goalService;
    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final CategoryRepository categoryRepository;
    private final TaskMapper taskMapper;
    private final SessionMapper sessionMapper;

    @Transactional
    public TaskInfoResponse createTask(UUID userId, TaskCreateRequest request) {
        Goal goal = (request.goalId() != null)
            ? goalRepository.findByIdAndUserId(request.goalId(), userId)
            .orElseThrow(() -> new GoalNotFoundException(request.goalId()))
            : goalService.getOrCreateInbox(userId);

        Integer duration = request.estimatedDuration();
        if (duration == null) {
            duration = findUser(userId).getPreferences().getPreferredSessionDuration();
        }

        Task task = Task.builder()
            .goal(goal)
            .title(request.title())
            .description(request.description())
            .estimatedDuration(duration)
            .mandatory(Boolean.TRUE.equals(request.mandatory()))
            .estimatedPoints(request.estimatedPoints() != null ? request.estimatedPoints() : 0)
            .allowTaskSplitting(Boolean.TRUE.equals(request.allowTaskSplitting()))
            .category(request.categoryId() != null ? categoryRepository.findById(request.categoryId()).orElse(null) : null)
            .status(TaskStatus.SCHEDULED)
            .build();

        return taskMapper.toInfoResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskWithSessionsResponse createTaskWithSessions(UUID userId, TaskWithSessionsRequest request) {
        TaskCreateRequest taskReq = request.task();
        Goal goal = (taskReq.goalId() != null)
            ? goalRepository.findByIdAndUserId(taskReq.goalId(), userId)
            .orElseThrow(() -> new GoalNotFoundException(taskReq.goalId()))
            : goalService.getOrCreateInbox(userId);

        Integer duration = taskReq.estimatedDuration();
        if (duration == null) {
            duration = findUser(userId).getPreferences().getPreferredSessionDuration();
        }

        Task task = Task.builder()
            .goal(goal)
            .title(taskReq.title())
            .description(taskReq.description())
            .estimatedDuration(duration)
            .mandatory(Boolean.TRUE.equals(taskReq.mandatory()))
            .estimatedPoints(taskReq.estimatedPoints() != null ? taskReq.estimatedPoints() : 0)
            .allowTaskSplitting(Boolean.TRUE.equals(taskReq.allowTaskSplitting()))
            .category(taskReq.categoryId() != null ? categoryRepository.findById(taskReq.categoryId()).orElse(null) : null)
            .status(TaskStatus.SCHEDULED)
            .build();

        for (var sessionReq : request.sessions()) {
            Zone zone = null;
            if (sessionReq.zoneId() != null) {
                zone = zoneRepository.findByIdAndUserId(sessionReq.zoneId(), userId)
                    .orElseThrow(() -> new ZoneNotFoundException(sessionReq.zoneId()));
            }
            validateSessionTimeRange(sessionReq.start(), sessionReq.end());
            Session session = Session.builder()
                .start(sessionReq.start())
                .end(sessionReq.end())
                .status(sessionReq.status() != null ? sessionReq.status() : SessionStatus.SCHEDULED)
                .zone(zone)
                .task(task)
                .build();
            task.getSessions().add(session);
        }

        Task saved = taskRepository.save(task);
        return new TaskWithSessionsResponse(
            taskMapper.toInfoResponse(saved),
            saved.getSessions().stream().map(sessionMapper::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<TaskInfoResponse> listTasksForGoal(UUID userId, UUID goalId) {
        return taskRepository.findByGoalIdAndGoalUserId(goalId, userId).stream()
            .map(taskMapper::toInfoResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public TaskInfoResponse getTask(UUID userId, UUID taskId) {
        return taskMapper.toInfoResponse(getOwnedTask(userId, taskId));
    }

    @Transactional
    public TaskInfoResponse updateTask(UUID userId, UUID taskId, TaskUpdateRequest request) {
        Task task = getOwnedTask(userId, taskId);
        if (request.title() != null) task.setTitle(request.title());
        if (request.description() != null) task.setDescription(request.description());
        if (request.estimatedDuration() != null) task.setEstimatedDuration(request.estimatedDuration());
        if (request.mandatory() != null) task.setMandatory(request.mandatory());
        if (request.estimatedPoints() != null) task.setEstimatedPoints(request.estimatedPoints());
        if (request.allowTaskSplitting() != null) task.setAllowTaskSplitting(request.allowTaskSplitting());
        if (request.categoryId() != null) task.setCategory(categoryRepository.findById(request.categoryId()).orElse(null));
        return taskMapper.toInfoResponse(task);
    }

    @Transactional
    public TaskInfoResponse moveTask(UUID userId, UUID taskId, TaskMoveRequest request) {
        Task task = getOwnedTask(userId, taskId);

        if (task.getGoal().getId().equals(request.goalId())) {
            return taskMapper.toInfoResponse(task);
        }

        if (!task.getDependsOn().isEmpty() || !task.getDependentTasks().isEmpty()) {
            throw new InvalidOperationException(
                "Cannot move a task that has dependency links (dependencies are same-goal only); remove them first");
        }

        Goal newGoal = goalRepository.findByIdAndUserId(request.goalId(), userId)
            .orElseThrow(() -> new GoalNotFoundException(request.goalId()));

        task.setGoal(newGoal);
        return taskMapper.toInfoResponse(task);
    }

    @Transactional
    public void deleteTask(UUID userId, UUID taskId, boolean cascade) {
        Task task = getOwnedTask(userId, taskId);
        boolean hasDependents = taskRepository.existsDependentsOf(taskId);
        if (hasDependents && !cascade) {
            throw new InvalidOperationException(
                "Task has other tasks depending on it; pass ?cascade=true to remove those links and delete");
        }
        if (hasDependents) {
            for (Task dependent : new HashSet<>(task.getDependentTasks())) {
                dependent.getDependsOn().remove(task);
            }
        }
        taskRepository.delete(task);
    }

    @Transactional
    public void addDependency(UUID userId, UUID taskId, TaskDependencyRequest request) {
        UUID dependsOnId = request.dependsOnTaskId();
        if (taskId.equals(dependsOnId)) {
            throw new TaskCyclicDependencyException("A task cannot depend on itself");
        }
        Task task = getOwnedTask(userId, taskId);
        Task dependsOn = getOwnedTask(userId, dependsOnId);

        if (!task.getGoal().getId().equals(dependsOn.getGoal().getId())) {
            throw new InvalidOperationException("Dependencies must be within the same goal");
        }
        if (wouldCreateCycle(taskId, dependsOnId)) {
            throw new TaskCyclicDependencyException("This dependency would create a cycle");
        }
        task.getDependsOn().add(dependsOn);
    }

    @Transactional
    public void removeDependency(UUID userId, UUID taskId, UUID dependsOnId) {
        Task task = getOwnedTask(userId, taskId);
        task.getDependsOn().removeIf(t -> t.getId().equals(dependsOnId));
    }

    @Transactional(readOnly = true)
    public List<TaskInfoResponse> listDependencies(UUID userId, UUID taskId) {
        Task task = getOwnedTask(userId, taskId);
        return task.getDependsOn().stream()
            .map(taskMapper::toInfoResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskInfoResponse> listDependents(UUID userId, UUID taskId) {
        Task task = getOwnedTask(userId, taskId);
        return task.getDependentTasks().stream()
            .map(taskMapper::toInfoResponse)
            .toList();
    }

    private void validateSessionTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new InvalidSessionTimeRangeException(start, end);
        }
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Task getOwnedTask(UUID userId, UUID taskId) {
        return taskRepository.findByIdAndGoalUserId(taskId, userId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private boolean wouldCreateCycle(UUID taskId, UUID proposedDependsOnId) {
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(proposedDependsOnId);
        while (!stack.isEmpty()) {
            UUID current = stack.pop();
            if (current.equals(taskId)) return true;
            if (!visited.add(current)) continue;
            stack.addAll(taskRepository.findDependsOnIds(current));
        }
        return false;
    }
}
