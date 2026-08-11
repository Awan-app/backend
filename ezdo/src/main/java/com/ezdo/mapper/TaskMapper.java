package com.ezdo.mapper;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.entity.Session;
import com.ezdo.entity.SessionStatus;
import com.ezdo.entity.Task;
import com.ezdo.entity.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final CategoryMapper categoryMapper;

    public TaskInfoResponse toInfoResponse(Task t) {
        Set<UUID> depIds = t.getDependsOn().stream()
            .map(Task::getId)
            .collect(Collectors.toSet());

        CategoryResponse category = t.getCategory() != null
            ? categoryMapper.toResponse(t.getCategory()) : null;

        return new TaskInfoResponse(
            t.getId(),
            t.getTitle(),
            t.getDescription(),
            t.getEstimatedDuration(),
            deriveStatus(t),
            t.getCompletedAt(),
            t.getMandatory(),
            t.getEstimatedPoints(),
            t.getAllowTaskSplitting(),
            t.getGoal().getId(),
            category,
            depIds
        );
    }

    public TaskStatus deriveStatus(Task t) {
        if (t.isCompleted()) return TaskStatus.COMPLETED;

        Set<Session> sessions = t.getSessions();
        if (sessions.isEmpty()) return TaskStatus.DRAFTED;
        if (sessions.stream().allMatch(s -> s.getStatus() == SessionStatus.CANCELLED)) {
            return TaskStatus.CANCELLED;
        }
        return TaskStatus.ACTIVE;
    }
}
