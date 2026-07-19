package com.ezdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TemplateOverrideRequest(
        String name,

        @NotNull(message = "Date is required")
        LocalDate dateOfDay,

        List<ZoneRequest> zones
) {
}
