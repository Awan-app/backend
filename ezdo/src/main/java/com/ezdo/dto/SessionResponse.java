package com.ezdo.dto;

import com.ezdo.entity.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        LocalDateTime start,
        LocalDateTime end,
        SessionStatus status,
        boolean locked,
        UUID zoneId,
        UUID taskId
) {
}
