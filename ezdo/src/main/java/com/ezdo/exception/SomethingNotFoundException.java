package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * This exception class is just for guidance as an example
 * for the upcoming exception classes.
 */
public class SomethingNotFoundException extends ApplicationException {
    
    public SomethingNotFoundException(String somethingId) {
        super(
            "Something with id=" + somethingId + " not found",
            HttpStatus.NOT_FOUND.value(),
            ErrorCodes.SOMETHING_NOT_FOUND,
            Map.of(
                "somethingId", somethingId
            )
        );
    }
}
