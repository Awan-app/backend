package com.ezdo.dto.ai.plan;

import com.ezdo.dto.task.SessionDraftRequest;
import com.ezdo.dto.task.TaskWithSessionsRequest;

import java.util.List;

/**
 * One proposed task, with the two scheduling channels kept apart so the client can
 * offer the user a choice rather than a decision already made.
 *
 * @param draft             the task plus whatever timing the source itself stated —
 *                          exactly the body to POST to {@code /api/v1/tasks/with-sessions}
 *                          to accept the user's own wording unchanged
 * @param aiProposedSessions the model's own suggestion, placed against real
 *                          availability; accept it by posting {@code draft} with its
 *                          {@code sessions} replaced by this list. Empty only when
 *                          the calendar left nowhere to put the task.
 * @param reason            why the model proposed those times — or, when nothing was
 *                          proposed, what stood in the way. Never blank.
 */
public record ProposedTask(
    TaskWithSessionsRequest draft,
    List<SessionDraftRequest> aiProposedSessions,
    String reason
) {
}
