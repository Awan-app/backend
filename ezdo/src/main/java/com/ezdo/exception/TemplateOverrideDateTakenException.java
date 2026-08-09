package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

/**
 * A user already has an override for that date. Only one override may exist per
 * user per date — the zone-resolution path assumes exactly one and would
 * otherwise fail with a NonUniqueResultException deep inside the scheduler.
 */
public class TemplateOverrideDateTakenException extends ApplicationException {

    public TemplateOverrideDateTakenException(LocalDate dateOfDay) {
        super(
            "An override already exists for " + dateOfDay,
            HttpStatus.CONFLICT.value(),
            ErrorCodes.TEMPLATE_OVERRIDE_DATE_TAKEN,
            Map.of("dateOfDay", dateOfDay)
        );
    }
}
