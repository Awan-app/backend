package com.ezdo.dto;

import com.ezdo.entity.SchedulingType;

import java.time.LocalTime;

public record PreferencesResponse(
    String timezone,
    Integer preferredSessionDuration,
    Integer bufferBetweenSessions,
    LocalTime wakeupTime,
    LocalTime sleepTime,
    SchedulingType schedulingType,
    Boolean dailySummaryEnabled
) {}
