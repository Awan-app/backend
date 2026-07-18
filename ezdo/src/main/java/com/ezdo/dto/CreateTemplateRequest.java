package com.ezdo.dto;

import java.time.DayOfWeek;
import java.util.Set;

public record CreateTemplateRequest (
        String name ,
        Set<DayOfWeek> daysOfWeek
){
}
