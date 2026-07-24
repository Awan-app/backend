package com.ezdo.dto.ai.enrich;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.NotBlank;

public record TaskEnrichmentRequest(
    @NotNull
    @NotBlank
    String title,

    String description
) {
}