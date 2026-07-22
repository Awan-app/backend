package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.util.Map;

public class ZoneOverlapException extends ApplicationException {

    public ZoneOverlapException(LocalTime startTime, LocalTime endTime) {
        super(
            "Zone time range overlaps with an existing zone: startTime=" + startTime + ", endTime=" + endTime,
            HttpStatus.CONFLICT.value(),
            ErrorCodes.ZONE_OVERLAP,
            Map.of(
                "startTime", startTime,
                "endTime", endTime
            )
        );
    }
}
