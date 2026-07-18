package com.ezdo.exception;

import java.time.LocalTime;
import java.util.Map;

public class InvalidZoneTimeRangeException extends ApplicationException {

    public InvalidZoneTimeRangeException(LocalTime startTime, LocalTime endTime) {
        super(
                "Invalid zone time range: startTime=" + startTime + ", endTime=" + endTime,
                400,
                ErrorCodes.VALIDATION_ERROR,
                Map.of(
                        "startTime", startTime,
                        "endTime", endTime
                )
        );
    }
}
