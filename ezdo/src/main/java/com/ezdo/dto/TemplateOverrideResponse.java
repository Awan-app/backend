package com.ezdo.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TemplateOverrideResponse(
        UUID id,
        String name,
        LocalDate dateOfDay,
        List<ZoneResponse> zones
) {
}
