package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

public class SessionNotFoundException extends ApplicationException {

    public SessionNotFoundException(UUID sessionId) {
        super(
                "Session with id=" + sessionId + " not found",
                404,
                ErrorCodes.SESSION_NOT_FOUND,
                Map.of(
                        "sessionId", sessionId
                )
        );
    }
}
