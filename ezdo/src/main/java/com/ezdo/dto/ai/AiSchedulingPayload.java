package com.ezdo.dto.ai;

import java.time.LocalDate;
import java.util.List;

public record AiSchedulingPayload(
    String goalTitle,
    String goalDescription,
    LocalDate startDate,
    LocalDate endDate,
    List<AiTaskItem> tasks,
    UserPreferencesContext preferences,
    List<DaySlot> calendar
) {}
