package com.ezdo.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends ApplicationException {

    public InvalidOperationException(String message) {
        super(
            message,
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.INVALID_OPERATION
        );
    }

    public InvalidOperationException() {
        super(
            "Invalid operation",
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.INVALID_OPERATION
        );
    }
}
