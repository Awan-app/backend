package com.ezdo.dto;

import com.ezdo.entity.SessionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    LocalDateTime start,
    LocalDateTime end,
    SessionStatus status,
    boolean locked,
    Instant firstCompletedAt,
    UUID zoneId,
    UUID taskId
) {
}
