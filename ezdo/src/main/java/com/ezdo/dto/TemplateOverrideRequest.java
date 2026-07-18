package com.ezdo.dto;

import java.time.LocalDate;

public record TemplateOverrideRequest(
        String name ,
        LocalDate dateOfDay

) {
}
