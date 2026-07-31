package com.ezdo.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TasksWithSessionsRequest(
    @NotEmpty(message = "At least one task is required")
    @Valid
    @Size(max = 50, message = "At most 50 tasks can be created at once")
    List<TaskWithSessionsRequest> tasks
) {
}
