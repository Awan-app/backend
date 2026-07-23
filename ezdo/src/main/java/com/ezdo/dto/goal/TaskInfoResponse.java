package com.ezdo.dto.goal;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.entity.TaskStatus;

import java.util.Set;
import java.util.UUID;

public record TaskInfoResponse(
    UUID id,
    String title,
    String description,
    Integer estimatedDuration,
    TaskStatus status,
    Boolean mandatory,
    Integer estimatedPoints,
    Boolean allowTaskSplitting,
    UUID goalId,
    CategoryResponse category,
    Set<UUID> dependsOnTaskIds
) {}
