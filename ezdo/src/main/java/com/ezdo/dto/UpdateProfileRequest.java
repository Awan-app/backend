package com.ezdo.dto;

import com.ezdo.entity.SchedulingType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateProfileRequest(
    @Size(max = 50)
    String firstName,

    @Size(max = 50)
    String lastName,

    @Past
    LocalDate birthDate,

    @Pattern(regexp = "^[A-Za-z]+(?:/[A-Za-z_]+)+$")
    String timezone,

    @Min(0)
    Integer preferredSessionDuration,

    @Min(0)
    Integer bufferBetweenSessions,

    LocalTime wakeupTime,

    LocalTime sleepTime,

    SchedulingType schedulingType,

    Boolean dailySummaryEnabled,

    Boolean notificationsEnabled
) {}
