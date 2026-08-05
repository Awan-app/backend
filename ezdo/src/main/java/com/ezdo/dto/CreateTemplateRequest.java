package com.ezdo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public record CreateTemplateRequest (
    @NotBlank(message = "Template name is required")
    String name,
    Set<DayOfWeek> daysOfWeek,
    @Valid
    List<ZoneRequest> zones
){
}
