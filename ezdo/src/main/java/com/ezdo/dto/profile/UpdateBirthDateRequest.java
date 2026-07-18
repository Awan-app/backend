package com.ezdo.dto.profile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UpdateBirthDateRequest(
    @Past
    @NotNull(message = "Birth date is required")
    LocalDate birthDate
) {}
