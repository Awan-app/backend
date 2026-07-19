package com.ezdo.dto.goal;

import com.ezdo.entity.GoalStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GoalUpdateRequest(
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    GoalStatus status,

    LocalDate targetDate
) {}
