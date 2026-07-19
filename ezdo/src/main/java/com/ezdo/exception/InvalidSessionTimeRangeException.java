package com.ezdo.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class InvalidSessionTimeRangeException extends ApplicationException{

    public InvalidSessionTimeRangeException(LocalDateTime start, LocalDateTime end) {
        super(
                "Invalid session time range: start=" + start + ", end=" + end,
                400,
                ErrorCodes.VALIDATION_ERROR,
                Map.of("start", start, "end", end)
        );
    }
}
