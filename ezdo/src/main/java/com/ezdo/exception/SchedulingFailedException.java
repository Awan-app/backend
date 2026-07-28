package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

public class SchedulingFailedException extends ApplicationException {

    public SchedulingFailedException(UUID goalId) {
        super(
                "Failed to generate a schedule.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCodes.SCHEDULING_FAILED,
                Map.of("goalId", goalId)
        );
    }
}