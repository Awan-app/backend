package com.ezdo.dto;

import java.time.DayOfWeek;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;

public record UpdateTemplateRequest (
    @NotBlank(message = "Template name is required")
    String name,

    Set<DayOfWeek> daysOfWeek
){
}
