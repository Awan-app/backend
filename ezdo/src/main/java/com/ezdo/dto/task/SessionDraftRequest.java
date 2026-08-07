package com.ezdo.dto.task;

import com.ezdo.entity.SessionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A session to create alongside a task.
 *
 * <p>The {@code status} field is retained only for API compatibility — sessions
 * are always created {@link SessionStatus#SCHEDULED}, so it is ignored by the
 * service. Lifecycle changes go through complete/cancel/uncomplete.
 */
public record SessionDraftRequest(
    UUID zoneId,

    @NotNull(message = "Start time is required")
    LocalDateTime start,

    @NotNull(message = "End time is required")
    LocalDateTime end,

    SessionStatus status
) {
    public SessionDraftRequest {
        if (status == null) status = SessionStatus.SCHEDULED;
    }
}
