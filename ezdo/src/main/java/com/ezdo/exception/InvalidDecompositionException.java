package com.ezdo.exception;

import org.springframework.http.HttpStatus;

public class InvalidDecompositionException extends ApplicationException {

    public InvalidDecompositionException(String message) {
        super(
            message,
            HttpStatus.UNPROCESSABLE_CONTENT.value(),
            ErrorCodes.INVALID_DECOMPOSITION
        );
    }
}
