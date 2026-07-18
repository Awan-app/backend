package com.ezdo.dto.profile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdateSleepScheduleRequest(
    @NotNull(message = "Wakeup time is required")
    LocalTime wakeupTime,

    @NotNull(message = "Sleep time is required")
    LocalTime sleepTime
) {}
