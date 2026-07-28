package com.ezdo.dto.ai.image;

import com.ezdo.dto.CategoryResponse;

import java.util.List;

/**
 * One task the planning model recovered from the vision report. Mirrors
 * {@code TaskEnrichmentResult} field-for-field, with two differences: {@code title}
 * IS the model's to produce here (there is no user-typed title to echo), and
 * {@code sessions} is a list rather than a single start/end pair, because one image
 * can carry a recurring commitment ("gym Mon & Wed 6-8pm").
 */
public record ExtractedTask(
    String title,
    String description,
    Integer estimatedDuration,
    Integer estimatedPoints,
    Boolean mandatory,
    Boolean allowTaskSplitting,
    CategoryResponse category,
    List<ExtractedSession> sessions
) {
}
