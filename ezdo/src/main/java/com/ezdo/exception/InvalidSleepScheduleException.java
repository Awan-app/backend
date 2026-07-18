package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.util.Map;

public class InvalidSleepScheduleException extends ApplicationException {

    public InvalidSleepScheduleException(LocalTime wakeupTime, LocalTime sleepTime) {
        super(
            "Wakeup time and sleep time cannot be the same.",
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.INVALID_SLEEP_SCHEDULE,
            Map.of(
                "wakeupTime", wakeupTime,
                "sleepTime", sleepTime
            )
        );
    }
}
