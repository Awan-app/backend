package com.ezdo.dto.task;

import com.ezdo.entity.SessionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

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
