package com.ezdo.dto.ai.schedule;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A proposed session that requires user confirmation before it is persisted.
 * Unlike confirmed in-zone sessions, these are returned inside
 * {@link GoalScheduleResponse#suggestions()} and are never written to the
 * database by the scheduling call itself.
 *
 * <p>Two varieties exist:
 * <ul>
 *   <li>{@link SuggestionType#NO_ZONE} — placed in an unzoned free gap;
 *       {@code zoneId} is {@code null}.</li>
 *   <li>{@link SuggestionType#OVERLAP} — placed over an existing unlocked
 *       session; {@code overlapInfo} carries details of what would be
 *       displaced.</li>
 * </ul>
 *
 * To accept a suggestion, pass it to
 * {@code POST /api/v1/ai/schedule/confirm}.
 */
public record SuggestedSession(
    UUID taskId,
    String taskTitle,
    UUID zoneId,             // null for NO_ZONE suggestions
    LocalDateTime start,
    LocalDateTime end,
    SuggestionType suggestionType,
    String reason,           // AI's explanation of why this placement was chosen
    OverlapInfo overlapInfo  // non-null only when suggestionType == OVERLAP
) {}
