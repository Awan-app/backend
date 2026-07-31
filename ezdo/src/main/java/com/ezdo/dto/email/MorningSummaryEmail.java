package com.ezdo.dto.email;

import java.time.LocalDate;
import java.util.List;

public record MorningSummaryEmail(
    String firstName,
    LocalDate date,
    List<SessionSummary> sessions,
    List<GoalSummary> activeGoals,
    List<DeadlineSummary> upcomingDeadlines,
    Integer totalFocusMinutes
) {}
