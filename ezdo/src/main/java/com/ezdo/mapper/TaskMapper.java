package com.ezdo.mapper;

import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.entity.Task;
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
            categoryMapper.toResponse(t.getCategory()),
            depIds
        );
    }
}
