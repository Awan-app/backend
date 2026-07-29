package com.ezdo.dto.ai.schedule;

import java.time.LocalDateTime;

public record ProposedSession(
        String taskId,
        String zoneId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}
