package com.ezdo.dto.ai;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GoalScheduleRequest(
    @NotNull UUID goalId
) {}
