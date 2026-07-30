package com.ezdo.dto.ai.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AiSchedulingPayload(
    String goalTitle,
    String goalDescription,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime currentDateTime,
    List<AiTaskItem> tasks,
    UserPreferencesContext preferences,
    List<DaySlot> calendar
) {}
