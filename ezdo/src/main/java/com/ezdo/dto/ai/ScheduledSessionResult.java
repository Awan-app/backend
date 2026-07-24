package com.ezdo.dto.ai;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduledSessionResult(
    UUID sessionId,
    UUID taskId,
    UUID zoneId,
    LocalDateTime start,
    LocalDateTime end
) {}
