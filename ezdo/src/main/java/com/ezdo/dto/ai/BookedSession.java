package com.ezdo.dto.ai;

import java.time.LocalTime;

public record BookedSession(
    LocalTime startTime,
    LocalTime endTime
) {}
