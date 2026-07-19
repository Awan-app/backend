package com.ezdo.mapper;

import com.ezdo.dto.goal.GoalInfoResponse;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.entity.Goal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GoalMapper {

    private final TaskMapper taskMapper;

    public GoalInfoResponse toInfoResponse(Goal g) {
        return toInfoResponse(g, true);
    }

    public GoalInfoResponse toInfoResponse(Goal g, boolean expandTasks) {
        List<TaskInfoResponse> tasks = expandTasks
            ? g.getTasks().stream()
                .map(taskMapper::toInfoResponse)
                .toList()
            : null;

        return new GoalInfoResponse(
            g.getId(),
            g.getTitle(),
            g.getDescription(),
            g.getStatus(),
            g.getTargetDate(),
            g.getCreatedAt(),
            Boolean.TRUE.equals(g.getInbox()),
            tasks
        );
    }
}
