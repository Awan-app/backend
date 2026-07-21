package com.ezdo.dto.ai;

import java.util.List;

public record TaskProposal(
    String tempId,
    String title,
    String description,
    Integer estimatedDuration,
    Integer estimatedPoints,
    boolean mandatory,
    boolean allowTaskSplitting,
    List<String> dependsOnTempIds
) {
}
