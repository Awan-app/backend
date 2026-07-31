package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class UnsupportedImageTypeException extends ApplicationException {

    public UnsupportedImageTypeException(String contentType, String supported) {
        super(
            "Unsupported image type"
                + (contentType != null ? ": " + contentType : "")
                + ". Supported types are " + supported,
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.UNSUPPORTED_IMAGE_TYPE,
            Map.of("supportedTypes", supported)
        );
    }
}
