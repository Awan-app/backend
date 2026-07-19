package com.ezdo.dto.goal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TaskCreateRequest(
    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    @Min(1)
    Integer estimatedDuration,

    Boolean mandatory,

    @Min(0)
    Integer estimatedPoints,

    Boolean allowTaskSplitting,

    UUID goalId // null -> lands in Inbox
) {
}
