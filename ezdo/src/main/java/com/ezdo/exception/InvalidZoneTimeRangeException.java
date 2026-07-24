package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.util.Map;

public class InvalidZoneTimeRangeException extends ApplicationException {

    public InvalidZoneTimeRangeException(LocalTime startTime, LocalTime endTime) {
        super(
            "Invalid zone time range: startTime=" + startTime + ", endTime=" + endTime,
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.INVALID_ZONE_TIME_RANGE,
            Map.of(
                    "startTime", startTime,
                    "endTime", endTime
            )
        );
    }
}
