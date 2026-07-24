package com.ezdo.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ZoneResponse(
        UUID id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        String color,
        CategoryResponse category,
        UUID templateId,
        UUID templateOverrideId
) {
}
