package com.ezdo.dto.ai.decompose;

import java.util.List;
import java.util.UUID;

public record TaskProposal(
    String tempId,
    String title,
    String description,
    Integer estimatedDuration,
    Integer estimatedPoints,
    boolean mandatory,
    boolean allowTaskSplitting,
    List<String> dependsOnTempIds,
    UUID categoryId
) {
}
