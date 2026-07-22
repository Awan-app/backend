package com.ezdo.exception;

import org.springframework.http.HttpStatus;

public class AiUnavailableException extends ApplicationException {

    public AiUnavailableException(Throwable cause) {
        super(
            "The AI assistant is temporarily unavailable, please try again",
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            ErrorCodes.AI_UNAVAILABLE
        );
        initCause(cause);
    }
}
