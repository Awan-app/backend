package com.ezdo.exception;

import java.util.Map;

public class OtpRateLimitException extends ApplicationException {

    public OtpRateLimitException(long retryAfterSeconds) {
        super(
            "Too many OTP requests. Please try again later.",
            429,
            ErrorCodes.OTP_RATE_LIMIT_EXCEEDED,
            Map.of("retryAfterSeconds", retryAfterSeconds)
        );
    }
}
