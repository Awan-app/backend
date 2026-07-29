package com.ezdo.dto.ai;

import java.time.LocalTime;

public record AvailableZone(
    String zoneId,
    String zoneName,
    LocalTime startTime,
    LocalTime endTime,
    long durationMinutes,
    String categoryName
) {}
