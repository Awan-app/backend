package com.ezdo.dto.ai.plan;

import com.ezdo.dto.CategoryResponse;

import java.util.List;

/**
 * One task the planning model recovered from the source text — a note the user
 * typed, or a report an image-reading model produced. The model owns every field
 * here, {@code title} included: a typed note mixes the action with incidental
 * context, so echoing it verbatim would make a poor title.
 *
 * <p>Sessions come in two independent channels that are never merged.
 * {@code sessions} holds only timing the source itself contained ("gym Mon &amp; Wed
 * 6-8pm") — a list rather than a single pair because one source can carry a
 * recurring commitment. {@code proposedSessions} is the model's own suggestion,
 * placed against the user's real availability and present even when the source said
 * nothing about timing, so the user always gets a second opinion rather than having
 * their explicit wording overruled.
 */
public record DraftedTask(
    String title,
    String description,
    Integer estimatedDuration,
    Integer estimatedPoints,
    Boolean mandatory,
    Boolean allowTaskSplitting,
    CategoryResponse category,
    List<DraftedSession> sessions,
    List<DraftedSession> proposedSessions,
    String reason
) {
}
