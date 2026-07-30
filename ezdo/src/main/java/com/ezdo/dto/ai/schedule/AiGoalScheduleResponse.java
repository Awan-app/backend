package com.ezdo.dto.ai.schedule;

import java.util.List;
import java.util.UUID;

/**
 * The AI scheduling result returned to the client.
 *
 * <p><strong>Nothing is persisted by the scheduling call itself.</strong>
 * The client reviews all three lists and calls
 * {@code POST /api/v1/ai/schedule/confirm} with the sessions it wants to save.
 *
 * <ul>
 *   <li>{@code proposedSessions} — sessions the AI placed inside a correctly
 *       matched zone with enough free time.</li>
 *   <li>{@code suggestions} — sessions the AI could only place outside a
 *       matched zone ({@link SuggestionType#NO_ZONE}) or by overlapping an
 *       existing unlocked session ({@link SuggestionType#OVERLAP}).</li>
 *   <li>{@code unscheduledTasks} — tasks the AI could not place anywhere even
 *       with suggestions (e.g. no free time in the horizon at all, or the only
 *       remaining time requires overlapping a locked session).</li>
 * </ul>
 */
public record AiGoalScheduleResponse(
    UUID goalId,
    List<ProposedSessionResult> proposedSessions,
    List<SuggestedSession> suggestions,
    List<UnscheduledTaskResult> unscheduledTasks
) {}
