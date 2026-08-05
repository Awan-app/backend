package com.ezdo.dto.ai.schedule;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A session that the AI placed inside a correctly matched zone, returned as
 * part of {@link GoalScheduleResponse#proposedSessions()}.
 *
 * <p>These sessions are <em>not yet persisted</em>. The entire schedule
 * (both {@code proposedSessions} and accepted {@code suggestions}) must be
 * confirmed by the user via {@code POST /api/v1/ai/schedule/confirm} before
 * anything is written to the database.
 */
public record ProposedSessionResult(
    UUID taskId,
    String taskTitle,
    UUID zoneId,
    LocalDateTime start,
    LocalDateTime end
) {}
