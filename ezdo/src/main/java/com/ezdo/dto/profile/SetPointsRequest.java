package com.ezdo.dto.profile;

import jakarta.validation.constraints.Min;

/**
 * Testing purposes only — sets the user's point balance to an absolute value.
 */
public record SetPointsRequest(
    @Min(value = 0, message = "Points cannot be negative")
    long points
) {}