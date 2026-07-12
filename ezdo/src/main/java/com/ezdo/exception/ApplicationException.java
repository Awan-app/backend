package com.ezdo.exception;

import lombok.Getter;

import java.util.Map;

/*
    response = {
        "message": ...,
        "statusCode": ...,
        "errorCode": ...,
        "timestamp": ...,
        "info": {
            "id": ...
        }
    }
*/

@Getter
public class ApplicationException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;
    private final Map<String, Object> info;

    public ApplicationException(String message, int statusCode, String errorCode) {
        this(message, statusCode, errorCode, Map.of());
    }

    public ApplicationException(String message, int statusCode, String errorCode, Map<String, Object> info) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.info = info;
    }
}
