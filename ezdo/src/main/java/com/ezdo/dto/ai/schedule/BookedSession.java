package com.ezdo.dto.ai.schedule;

import java.time.LocalTime;

public record BookedSession(
    LocalTime startTime,
    LocalTime endTime
) {}
