package com.ezdo.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TaskCreateRequest(
    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    Integer estimatedDuration,

    Boolean mandatory,

    Integer estimatedPoints,

    Boolean allowTaskSplitting,

    UUID goalId // null -> lands in Inbox
) {}
