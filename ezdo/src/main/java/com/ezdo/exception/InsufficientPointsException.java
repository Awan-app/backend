package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class InsufficientPointsException extends ApplicationException {

    public InsufficientPointsException(int currentPoints, int requestedPoints) {
        super(
            "Insufficient points.",
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.INSUFFICIENT_POINTS,
            Map.of(
                "currentPoints", currentPoints,
                "requestedPoints", requestedPoints
            )
        );
    }
}
