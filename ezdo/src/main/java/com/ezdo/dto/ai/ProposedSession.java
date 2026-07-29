package com.ezdo.dto.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ProposedSession(
        String taskId,
        String zoneId,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}
