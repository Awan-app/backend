package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

public class SessionLockedException extends ApplicationException{

    public SessionLockedException(UUID sessionId) {
        super(
                "Session with id=" + sessionId + " is locked and cannot be modified",
                400,
                ErrorCodes.VALIDATION_ERROR,
                Map.of("sessionId", sessionId)
        );
    }
}
