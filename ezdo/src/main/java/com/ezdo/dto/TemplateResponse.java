package com.ezdo.dto;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TemplateResponse (
        UUID id,
        String name,
        Set<DayOfWeek>daysOfWeek,
        List<ZoneResponse> zones
){
}
