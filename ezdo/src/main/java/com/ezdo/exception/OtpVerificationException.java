package com.ezdo.exception;

import java.util.Map;

public class OtpVerificationException extends ApplicationException {

    public OtpVerificationException(String message, String errorCode) {
        super(message, 400, errorCode);
    }

    public OtpVerificationException(String message, String errorCode, Map<String, Object> info) {
        super(message, 400, errorCode, info);
    }
}
