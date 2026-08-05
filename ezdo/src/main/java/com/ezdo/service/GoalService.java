package com.ezdo.service;

import com.ezdo.dto.goal.*;
import com.ezdo.dto.task.BulkAddTasksRequest;
import com.ezdo.dto.task.BulkTaskItem;
import com.ezdo.entity.*;
import com.ezdo.exception.*;
import com.ezdo.mapper.GoalMapper;
import com.ezdo.mapper.TaskMapper;
import com.ezdo.repository.CategoryRepository;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.TaskRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.util.DependencyGraphValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final GoalMapper goalMapper;
    private final TaskMapper taskMapper;

    @Transactional
    public GoalInfoResponse createGoal(UUID userId, GoalCreateRequest request) {
        if (!request.tasks().isEmpty()) {
            DependencyGraphValidator.assertNoCycles(request.tasks());
        }

        Goal goal = Goal.builder()
            .user(findUser(userId))
            .title(request.title())
            .description(request.description())
            .targetDate(request.targetDate())
            .status(GoalStatus.ACTIVE)
            .inbox(false)
            .build();

        Integer defaultDuration = findUser(userId).getPreferences().getPreferredSessionDuration();
        Map<String, Task> byTempId = new LinkedHashMap<>();
        for (DraftTaskRequest dt : request.tasks()) {
            if (byTempId.containsKey(dt.tempId())) {
                throw new DuplicateTempIdException(dt.tempId());
            }
            Category category = dt.categoryId() != null
                ? categoryRepository.findByIdAndUserId(dt.categoryId(), userId)
                    .orElseThrow(() -> new CategoryNotFoundException(dt.categoryId()))
                : null;
            Task task = Task.builder()
                .goal(goal)
                .title(dt.title())
                .description(dt.description())
                .estimatedDuration(dt.estimatedDuration() != null ? dt.estimatedDuration() : defaultDuration)
                .mandatory(Boolean.TRUE.equals(dt.mandatory()))
                .estimatedPoints(dt.estimatedPoints() != null ? dt.estimatedPoints() : 0)
                .allowTaskSplitting(Boolean.TRUE.equals(dt.allowTaskSplitting()))
                .category(category)
                .status(TaskStatus.SCHEDULED)
                .build();
            goal.getTasks().add(task);
            byTempId.put(dt.tempId(), task);
        }

        for (DraftTaskRequest dt : request.tasks()) {
            Task task = byTempId.get(dt.tempId());
            for (String depTempId : Optional.ofNullable(dt.dependsOnTempIds()).orElse(List.of())) {
                Task dep = byTempId.get(depTempId);
                if (dep == null) throw new UnknownTempIdException(depTempId);
                if (dep == task) throw new TaskCyclicDependencyException("A task cannot depend on itself");
                task.getDependsOn().add(dep);
            }
        }

        return goalMapper.toInfoResponse(goalRepository.save(goal), !request.tasks().isEmpty());
    }

    @Transactional(readOnly = true)
    public Page<GoalInfoResponse> listGoals(UUID userId,
                                            GoalStatus status,
                                            boolean includeInbox,
                                            boolean expand,
                                            Pageable pageable) {
        Page<Goal> page;
        if (includeInbox) {
            page = status != null
                ? goalRepository.findByUserIdAndStatus(userId, status, pageable)
                : goalRepository.findByUserId(userId, pageable);
        } else {
            page = status != null
                ? goalRepository.findByUserIdAndStatusAndInboxFalse(userId, status, pageable)
                : goalRepository.findByUserIdAndInboxFalse(userId, pageable);
        }
        return page.map(g -> goalMapper.toInfoResponse(g, expand));
    }

    @Transactional(readOnly = true)
    public GoalInfoResponse getGoal(UUID userId, UUID goalId, boolean expand) {
        Goal goal = findGoal(goalId, userId);
        return goalMapper.toInfoResponse(goal, expand);
    }

    @Transactional
    public GoalInfoResponse updateGoal(UUID userId, UUID goalId, GoalUpdateRequest req) {
        Goal goal = findGoal(goalId, userId);
        if (Boolean.TRUE.equals(goal.getInbox())) {
            throw new InvalidOperationException("The Inbox goal cannot be edited");
        }
        if (req.title() != null) goal.setTitle(req.title());
        if (req.description() != null) goal.setDescription(req.description());
        if (req.status() != null) goal.setStatus(req.status());
        if (req.targetDate() != null) goal.setTargetDate(req.targetDate());
        return goalMapper.toInfoResponse(goalRepository.save(goal), false);
    }

    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        Goal goal = findGoal(goalId, userId);
        if (Boolean.TRUE.equals(goal.getInbox())) {
            throw new InvalidOperationException("The Inbox goal cannot be deleted");
        }

        goalRepository.delete(goal); // cascades tasks via orphanRemoval
    }

    @Transactional
    public List<TaskInfoResponse> bulkAddTasks(UUID userId,
                                               UUID goalId,
                                               BulkAddTasksRequest request) {
        Goal goal = findGoal(goalId, userId);

        List<Task> existingTasks = taskRepository.findByGoalIdAndGoalUserId(goalId, userId);
        Map<UUID, Task> existingById = existingTasks.stream()
            .collect(Collectors.toMap(Task::getId, t -> t));

        Integer defaultDuration = findUser(userId).getPreferences().getPreferredSessionDuration();
        Map<String, Task> newByTempId = new LinkedHashMap<>();
        for (BulkTaskItem item : request.tasks()) {
            if (newByTempId.containsKey(item.tempId())) {
                throw new DuplicateTempIdException(item.tempId());
            }
            Category category = item.categoryId() != null
                ? categoryRepository.findByIdAndUserId(item.categoryId(), userId)
                    .orElseThrow(() -> new CategoryNotFoundException(item.categoryId()))
                : null;
            newByTempId.put(item.tempId(), Task.builder()
                .goal(goal)
                .title(item.title())
                .description(item.description())
                .estimatedDuration(item.estimatedDuration() != null ? item.estimatedDuration() : defaultDuration)
                .mandatory(Boolean.TRUE.equals(item.mandatory()))
                .estimatedPoints(item.estimatedPoints() != null ? item.estimatedPoints() : 0)
                .allowTaskSplitting(Boolean.TRUE.equals(item.allowTaskSplitting()))
                .category(category)
                .status(TaskStatus.SCHEDULED)
                .build());
        }

        Map<String, List<String>> graph = new HashMap<>();
        for (Task t : existingTasks) {
            graph.put(t.getId().toString(),
                t.getDependsOn().stream().map(d -> d.getId().toString()).toList());
        }
        for (BulkTaskItem item : request.tasks()) {
            graph.put(item.tempId(), Optional.ofNullable(item.dependsOnRefs()).orElse(List.of()));
        }

        for (BulkTaskItem item : request.tasks()) {
            Task task = newByTempId.get(item.tempId());
            for (String ref : Optional.ofNullable(item.dependsOnRefs()).orElse(List.of())) {
                Task dep = newByTempId.get(ref);
                if (dep == null) {
                    UUID depId = parseUuidOrNull(ref);
                    dep = (depId != null) ? existingById.get(depId) : null;
                    if (dep == null) throw new InvalidOperationException("Unknown dependency reference: " + ref);
                }
                if (dep == task) throw new InvalidOperationException("A task cannot depend on itself");
                task.getDependsOn().add(dep);
            }
        }

        DependencyGraphValidator.assertNoCycles(graph);

        return taskRepository.saveAll(newByTempId.values()).stream()
            .map(taskMapper::toInfoResponse)
            .toList();
    }

    private UUID parseUuidOrNull(String s) {
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    @Transactional
    public Goal getOrCreateInbox(UUID userId) {
        return goalRepository.findByUserIdAndInboxTrue(userId)
            .orElseGet(() -> goalRepository.save(
                Goal.builder()
                    .user(findUser(userId))
                    .title("Inbox")
                    .status(GoalStatus.ACTIVE)
                    .inbox(true)
                    .build()));
    }

    @Transactional
    public GoalInfoResponse getOrCreateInboxResponse(UUID userId) {
        return goalMapper.toInfoResponse(getOrCreateInbox(userId));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Goal findGoal(UUID goalId, UUID userId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
            .orElseThrow(() -> new GoalNotFoundException(goalId));
    }
}
