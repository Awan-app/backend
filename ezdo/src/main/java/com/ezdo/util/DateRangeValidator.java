package com.ezdo.util;

import com.ezdo.exception.InvalidOperationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateRangeValidator {

    public static final int MAX_RANGE_DAYS = 31;

    private DateRangeValidator() {}

    public static void validate(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidOperationException("endDate must not be before startDate");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new InvalidOperationException(
                "Date range must not exceed " + MAX_RANGE_DAYS + " days; requested " + days);
        }
    }
}
