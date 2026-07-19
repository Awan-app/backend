package com.ezdo.dto;

import com.ezdo.entity.SessionStatus;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record SessionRequest(
        @NotNull(message = "Start time is required")
        LocalDateTime start,

        @NotNull(message = "End time is required")
        LocalDateTime end,

        SessionStatus status
) {
}
