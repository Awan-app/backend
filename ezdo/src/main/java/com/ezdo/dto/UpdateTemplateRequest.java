package com.ezdo.dto;

import java.time.DayOfWeek;
import java.util.Set;

public record UpdateTemplateRequest (
        String name,
        Set<DayOfWeek> daysOfWeek
){
}
