package com.ezdo.dto.email;

import java.time.LocalTime;

public record SessionSummary(
    String title,
    LocalTime startTime,
    LocalTime endTime
) {}
