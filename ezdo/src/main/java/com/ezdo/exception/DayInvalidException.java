package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Set;

public class DayInvalidException extends ApplicationException{

    public DayInvalidException(Set<DayOfWeek> days) {
        super(
                "The following days already belong to another weekly template" + days + " not found",
                HttpStatus.CONFLICT.value(),
                ErrorCodes.DAY_ALREADY_ASSIGNED,
                Map.of(
                        "days", days
                )
        );
    }
}
