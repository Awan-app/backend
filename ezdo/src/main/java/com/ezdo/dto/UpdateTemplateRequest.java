package com.ezdo.dto;

import java.time.DayOfWeek;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UpdateTemplateRequest (
        @NotBlank(message = "Template name is required")
        String name,

        Set<DayOfWeek> daysOfWeek
){
}
