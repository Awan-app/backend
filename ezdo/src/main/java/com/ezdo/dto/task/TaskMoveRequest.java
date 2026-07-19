package com.ezdo.dto.task;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TaskMoveRequest(
    @NotNull
    UUID goalId
) {
}
