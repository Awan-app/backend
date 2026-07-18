package com.ezdo.dto.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GoalCreateRequest(
    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    LocalDate targetDate
) {}
