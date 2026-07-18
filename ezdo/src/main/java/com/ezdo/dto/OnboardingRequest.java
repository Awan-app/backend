package com.ezdo.dto;

import com.ezdo.entity.SchedulingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record OnboardingRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate,

        @NotBlank(message = "Timezone is required")
        String timezone,

        @NotNull(message = "Preferred session duration is required")
        @Min(0)
        Integer preferredSessionDuration,

        @NotNull(message = "Buffer between sessions is required")
        @Min(0)
        Integer bufferBetweenSessions,

        @NotNull(message = "Wakeup time is required")
        LocalTime wakeupTime,

        @NotNull(message = "Sleep time is required")
        LocalTime sleepTime,

        SchedulingType schedulingType
) {
    public OnboardingRequest {
        if (schedulingType == null) {
            schedulingType = SchedulingType.BALANCED;
        }
    }
}
