package com.ezdo.dto.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BulkTaskItem(
    @NotBlank
    String tempId,

    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    @Min(1)
    Integer estimatedDuration,

    Boolean mandatory,

    @Min(0)
    Integer estimatedPoints,

    Boolean allowTaskSplitting,

    UUID categoryId,

    List<String> dependsOnRefs // each entry: a tempId from THIS batch, OR a real UUID of an existing task in the goal
) {}
