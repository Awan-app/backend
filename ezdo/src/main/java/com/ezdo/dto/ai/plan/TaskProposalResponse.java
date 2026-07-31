package com.ezdo.dto.ai.plan;

import java.time.Instant;
import java.util.List;

/**
 * A proposal, not a creation — nothing here has been persisted. Every element of
 * {@code tasks} carries a {@code draft} that is literally the body the client posts
 * back to {@code POST /api/v1/tasks/with-sessions} to accept that task, so confirming
 * needs no mapping on the client side.
 *
 * <p>Returned by both AI drafting endpoints, so a client renders one shape whether
 * the user typed a note or uploaded an image.
 *
 * @param sourceSummary what the vision stage read out of an uploaded image, surfaced
 *                      so the UI can show what was understood (and so a bad
 *                      extraction is diagnosable without server logs). Null when the
 *                      source was text the user typed — there is nothing to summarise.
 * @param tasks         the proposed drafts, empty when the source held nothing actionable
 */
public record TaskProposalResponse(
    String sourceSummary,
    List<ProposedTask> tasks,
    Instant timestamp
) {
}
