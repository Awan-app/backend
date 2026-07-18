package com.ezdo.dto.profile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionSettingsRequest(
    @NotNull(message = "Preferred session duration is required")
    @Min(value = 10, message = "Session duration mustn't be less than 10 min")
    Integer preferredSessionDuration,

    @NotNull(message = "Buffer between sessions is required")
    @Min(0)
    Integer bufferBetweenSessions
) {}
