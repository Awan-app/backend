package com.ezdo.dto.ai.schedule;

/**
 * Describes why a proposed session is a "suggestion" rather than a confirmed
 * in-zone placement.
 *
 * <ul>
 *   <li>{@link #NO_ZONE} — the AI placed the task outside any zone (unzoned
 *       free gap) because no matching zone had sufficient free time.</li>
 *   <li>{@link #OVERLAP} — the AI overlapped an existing unlocked session as a
 *       last resort because no free time of any kind was available.</li>
 * </ul>
 *
 * Sessions of either type are returned in
 * {@link GoalScheduleResponse#suggestions()} and are <em>not persisted</em>
 * until the user confirms via {@code POST /api/v1/ai/schedule/confirm}.
 */
public enum SuggestionType {
    NO_ZONE,
    OVERLAP
}
