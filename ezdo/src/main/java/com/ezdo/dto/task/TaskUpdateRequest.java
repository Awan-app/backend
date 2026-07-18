package com.ezdo.dto.task;

import com.ezdo.entity.TaskStatus;
import jakarta.validation.constraints.Size;

public record TaskUpdateRequest(
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    Integer estimatedDuration,

    TaskStatus status,

    Boolean mandatory,

    Integer estimatedPoints,

    Boolean allowTaskSplitting
) {}
