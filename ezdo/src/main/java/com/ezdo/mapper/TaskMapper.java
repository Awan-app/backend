package com.ezdo.mapper;

import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.entity.Task;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TaskMapper {

    public TaskInfoResponse toInfoResponse(Task t) {
        Set<UUID> depIds = t.getDependsOn().stream()
            .map(Task::getId)
            .collect(Collectors.toSet());

        return new TaskInfoResponse(
            t.getId(),
            t.getTitle(),
            t.getDescription(),
            t.getEstimatedDuration(),
            t.getStatus(),
            t.getMandatory(),
            t.getEstimatedPoints(),
            t.getAllowTaskSplitting(),
            t.getGoal().getId(),
            depIds
        );
    }
}
