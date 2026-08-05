package com.ezdo.dto.task;

import java.util.List;

public record TasksWithSessionsResponse(
    List<TaskWithSessionsResponse> tasks
) {
}
