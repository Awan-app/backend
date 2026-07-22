package com.ezdo.dto.ai;

import java.util.List;

public record AiTaskItem(
    String taskId,
    String title,
    String description,
    int estimatedDurationMinutes,
    int estimatedPoints,
    boolean mandatory,
    boolean allowSplitting,
    List<String> dependsOnTaskIds
) {}
