package com.ezdo.dto.ai;

import java.time.LocalDate;
import java.time.LocalTime;

public record ProposedSession(
    String taskId,
    String zoneId,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime
) {}
