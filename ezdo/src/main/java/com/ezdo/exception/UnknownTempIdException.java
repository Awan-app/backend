package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class UnknownTempIdException extends ApplicationException {

    public UnknownTempIdException(String tempId) {
        super(
            "Unknown tempId: " + tempId,
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.UNKNOWN_TEMP_ID,
            Map.of(
                "tempId", tempId
            )
        );
    }
}
