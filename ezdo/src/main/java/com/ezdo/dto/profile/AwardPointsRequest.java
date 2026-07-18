package com.ezdo.dto.profile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AwardPointsRequest(

    @NotNull
    @Positive
    Integer points
) {}
