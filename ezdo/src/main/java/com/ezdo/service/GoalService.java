package com.ezdo.service;

import com.ezdo.dto.goal.*;
import com.ezdo.entity.*;
import com.ezdo.exception.*;
import com.ezdo.mapper.GoalMapper;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final GoalMapper goalMapper;

    @Transactional
    public GoalInfoResponse createGoal(UUID userId, GoalCreateRequest request) {
        Goal goal = Goal.builder()
            .user(findUser(userId))
            .title(request.title())
            .description(request.description())
            .targetDate(request.targetDate())
            .status(GoalStatus.ACTIVE)
            .inbox(false)
            .build();

        return goalMapper.toInfoResponse(goalRepository.save(goal), false);
    }

    @Transactional(readOnly = true)
    public Page<GoalInfoResponse> listGoals(UUID userId, GoalStatus status, boolean includeInbox, Pageable pageable) {
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
        return page.map(g -> goalMapper.toInfoResponse(g, false));
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
        return goalMapper.toInfoResponse(goal, false);
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

    @Transactional
    public GoalInfoResponse confirmDraft(UUID userId, ConfirmGoalRequest request) {
        assertNoCycles(request.tasks());

        Goal goal = Goal.builder()
            .user(findUser(userId))
            .title(request.title())
            .description(request.description())
            .targetDate(request.targetDate())
            .status(GoalStatus.ACTIVE)
            .inbox(false)
            .build();

        Map<String, Task> byTempId = new LinkedHashMap<>();
        for (DraftTaskRequest dt : request.tasks()) {
            if (byTempId.containsKey(dt.tempId())) {
                throw new DuplicateTempIdException(dt.tempId());
            }
            Task task = Task.builder()
                .goal(goal)
                .title(dt.title())
                .description(dt.description())
                .estimatedDuration(dt.estimatedDuration())
                .mandatory(Boolean.TRUE.equals(dt.mandatory()))
                .estimatedPoints(dt.estimatedPoints() != null ? dt.estimatedPoints() : 0)
                .allowTaskSplitting(Boolean.TRUE.equals(dt.allowTaskSplitting()))
                .status(TaskStatus.SCHEDULED)
                .build();
            goal.getTasks().add(task);
            byTempId.put(dt.tempId(), task);
        }

        for (DraftTaskRequest dt : request.tasks()) {
            Task task = byTempId.get(dt.tempId());
            List<String> deps = dt.dependsOnTempIds() == null ? List.of() : dt.dependsOnTempIds();
            for (String depTempId : deps) {
                Task dep = byTempId.get(depTempId);
                if (dep == null) {
                    throw new UnknownTempIdException(depTempId);
                }
                if (dep == task) {
                    throw new TaskCyclicDependencyException("A task cannot depend on itself");
                }
                task.getDependsOn().add(dep);
            }
        }

        return goalMapper.toInfoResponse(goalRepository.save(goal));
    }

    private void assertNoCycles(List<DraftTaskRequest> tasks) {
        Map<String, List<String>> graph = new HashMap<>();
        for (DraftTaskRequest dt : tasks) {
            graph.put(dt.tempId(), dt.dependsOnTempIds() == null ? List.of() : dt.dependsOnTempIds());
        }
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        for (String start : graph.keySet()) {
            if (hasCycleDfs(start, graph, visited, inStack)) {
                throw new TaskCyclicDependencyException();
            }
        }
    }

    private boolean hasCycleDfs(
        String node,
        Map<String, List<String>> graph,
        Set<String> visited,
        Set<String> inStack
    ) {
        if (inStack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        inStack.add(node);
        for (String next : graph.getOrDefault(node, List.of())) {
            if (hasCycleDfs(next, graph, visited, inStack)) return true;
        }
        inStack.remove(node);
        return false;
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
