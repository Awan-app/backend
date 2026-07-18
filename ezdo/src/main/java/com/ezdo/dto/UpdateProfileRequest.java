package com.ezdo.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

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
        Integer bufferBetweenSessions
) {}