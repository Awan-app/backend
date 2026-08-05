package com.ezdo.dto.email;

import java.time.LocalDate;

public record DeadlineSummary(
    String title,
    LocalDate deadline,
    long daysRemaining
) {}
