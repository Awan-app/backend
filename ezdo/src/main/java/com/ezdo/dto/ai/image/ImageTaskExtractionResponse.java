package com.ezdo.dto.ai.image;

import com.ezdo.dto.task.TaskWithSessionsRequest;

import java.time.Instant;
import java.util.List;

/**
 * A proposal, not a creation — nothing here has been persisted. Each element of
 * {@code tasks} is literally the body the client posts back to
 * {@code POST /api/v1/tasks/with-sessions} to accept that task, so confirming
 * needs no mapping on the client side.
 *
 * @param imageSummary what the vision stage read out of the image, surfaced so the
 *                     UI can show the user what was understood (and so a bad
 *                     extraction is diagnosable without server logs)
 * @param tasks        the proposed drafts, empty when the image held nothing actionable
 */
public record ImageTaskExtractionResponse(
    String imageSummary,
    List<TaskWithSessionsRequest> tasks,
    Instant timestamp
) {
}
