package com.ezdo.dto.task;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TaskDependencyRequest(
    @NotNull
    UUID dependsOnTaskId
) {}
