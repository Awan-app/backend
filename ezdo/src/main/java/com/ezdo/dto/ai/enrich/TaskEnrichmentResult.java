package com.ezdo.dto.ai.enrich;

import com.ezdo.dto.CategoryResponse;

import java.time.LocalDateTime;

/**
 * The shape Spring AI's structured-output converter asks the model to fill in.
 * Excludes title (never the model's to produce — always echoed from the request
 * verbatim). Includes description, but only as a fallback: the model should return
 * null here when the user already supplied one, since TaskEnrichmentService keeps
 * the user's original text untouched in that case.
 */
public record TaskEnrichmentResult(
    String description,
    Integer estimatedDuration,
    Integer estimatedPoints,
    Boolean mandatory,
    Boolean allowTaskSplitting,
    CategoryResponse category,
    LocalDateTime scheduledStart,
    LocalDateTime scheduledEnd
) {
}