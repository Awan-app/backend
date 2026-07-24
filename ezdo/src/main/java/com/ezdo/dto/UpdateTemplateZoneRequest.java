package com.ezdo.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateTemplateZoneRequest(
        @NotEmpty(message = "At least one zone is required")
        List<ZoneRequest> zones
) {
}
