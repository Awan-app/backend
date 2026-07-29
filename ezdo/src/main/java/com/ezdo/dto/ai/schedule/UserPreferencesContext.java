package com.ezdo.dto.ai.schedule;

import java.time.LocalTime;

public record UserPreferencesContext(
    int preferredSessionDurationMinutes,
    int bufferBetweenSessionsMinutes,
    LocalTime wakeupTime,
    LocalTime sleepTime,
    String schedulingStrategy
) {}
