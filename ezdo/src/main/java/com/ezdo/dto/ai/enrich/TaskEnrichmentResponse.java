package com.ezdo.dto.ai.enrich;

import com.ezdo.dto.CategoryResponse;

/** A single-task proposal returned to the caller for review before creation. */
public record TaskEnrichmentResponse(
    String title,
    String description,
    Integer estimatedDuration,
    Integer estimatedPoints,
    boolean mandatory,
    boolean allowTaskSplitting,
    CategoryResponse category
) {
}