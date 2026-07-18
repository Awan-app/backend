package com.ezdo.dto;

import java.time.LocalTime;

public record ZoneRequest(
        String name,
        LocalTime startTime,
        LocalTime endTime,
        String color
) {
}
