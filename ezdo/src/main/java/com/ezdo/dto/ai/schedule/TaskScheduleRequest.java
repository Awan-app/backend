package com.ezdo.dto.ai.schedule;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TaskScheduleRequest(
    @NotNull UUID taskId,
    Integer horizonDays
) {}