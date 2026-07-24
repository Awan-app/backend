package com.ezdo.dto.ai.decompose;

import com.ezdo.dto.CategoryResponse;

import java.util.List;

public record TaskProposal(
    String tempId,
    String title,
    String description,
    Integer estimatedDuration,
    Integer estimatedPoints,
    boolean mandatory,
    boolean allowTaskSplitting,
    CategoryResponse category,
    List<String> dependsOnTempIds
) {
}
