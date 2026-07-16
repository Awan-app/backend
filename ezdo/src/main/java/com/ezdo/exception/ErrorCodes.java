package com.ezdo.exception;

public class ErrorCodes {

    private ErrorCodes() {}

    public static final String SOMETHING_NOT_FOUND = "SOMETHING_NOT_FOUND";

    // Auth - OTP
    public static final String OTP_RATE_LIMIT_EXCEEDED = "OTP_RATE_LIMIT_EXCEEDED";
    public static final String OTP_EXPIRED_OR_NOT_FOUND = "OTP_EXPIRED_OR_NOT_FOUND";
    public static final String OTP_INVALID_CODE = "OTP_INVALID_CODE";
    public static final String OTP_LOCKED = "OTP_LOCKED";

    // Auth - Refresh Token
    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";
    public static final String REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";
    public static final String REFRESH_TOKEN_REUSE_DETECTED = "REFRESH_TOKEN_REUSE_DETECTED";
}
