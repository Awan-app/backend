package com.ezdo.dto.ai.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskPlanningRequest(
    @NotBlank(message = "Text is required")
    @Size(max = 4000, message = "Text must be at most 4000 characters")
    String text
) {
}
