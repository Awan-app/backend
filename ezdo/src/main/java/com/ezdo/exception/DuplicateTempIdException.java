package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class DuplicateTempIdException extends ApplicationException {

    public DuplicateTempIdException(String tempId) {
        super(
            "Duplicate tempId: " + tempId,
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.DUPLICATE_TEMP_ID,
            Map.of(
                "tempId", tempId
            )
        );
    }
}
