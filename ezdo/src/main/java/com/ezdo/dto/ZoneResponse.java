package com.ezdo.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ZoneResponse(
        UUID id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        String color,
        UUID templateId,
        UUID templateOverrideId
) {
}
