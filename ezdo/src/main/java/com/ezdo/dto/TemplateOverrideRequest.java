package com.ezdo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TemplateOverrideRequest(
    @NotBlank(message = "Template override name is required")
    String name,

    @NotNull(message = "Date is required")
    LocalDate dateOfDay,

    @Valid
    List<ZoneRequest> zones
) {
}
