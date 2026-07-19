package com.ezdo.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkAddTasksRequest(
    @NotEmpty
    @Valid
    @Size(max = 50)
    List<BulkTaskItem> tasks
) {}
