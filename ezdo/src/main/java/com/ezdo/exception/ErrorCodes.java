package com.ezdo.exception;

public class ErrorCodes {

    private ErrorCodes() {}

    public static final String SOMETHING_NOT_FOUND = "SOMETHING_NOT_FOUND";

    // Request / validation
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MALFORMED_REQUEST_BODY = "MALFORMED_REQUEST_BODY";
    public static final String MISSING_PARAMETER = "MISSING_PARAMETER";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String MEDIA_TYPE_NOT_SUPPORTED = "MEDIA_TYPE_NOT_SUPPORTED";
    public static final String INVALID_OPERATION = "INVALID_OPERATION";
    public static final String TASK_CYCLIC_DEPENDENCY = "TASK_CYCLIC_DEPENDENCY";
    public static final String DUPLICATE_TEMP_ID = "DUPLICATE_TEMP_ID";
    public static final String UNKNOWN_TEMP_ID = "UNKNOWN_TEMP_ID";
    public static final String INVALID_SLEEP_SCHEDULE = "INVALID_SLEEP_SCHEDULE";
    public static final String INSUFFICIENT_POINTS = "INSUFFICIENT_POINTS";

    // Security
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";

    // Persistence
    public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";

    // Onboarding / User
    public static final String ONBOARDING_ALREADY_COMPLETED = "ONBOARDING_ALREADY_COMPLETED";
    public static final String INVALID_TIMEZONE = "INVALID_TIMEZONE";

    // Fallback
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    // Auth - OTP
    public static final String OTP_RATE_LIMIT_EXCEEDED = "OTP_RATE_LIMIT_EXCEEDED";
    public static final String OTP_EXPIRED_OR_NOT_FOUND = "OTP_EXPIRED_OR_NOT_FOUND";
    public static final String OTP_INVALID_CODE = "OTP_INVALID_CODE";
    public static final String OTP_LOCKED = "OTP_LOCKED";

    // Auth - Refresh Token
    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";
    public static final String REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";
    public static final String REFRESH_TOKEN_REUSE_DETECTED = "REFRESH_TOKEN_REUSE_DETECTED";

    //zone-template
    public static final String ZONE_OVERLAP = "ZONE_OVERLAP";
    public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
    public static final String TEMPLATE_OVERRIDE_NOT_FOUND = "TEMPLATE_OVERRIDE_NOT_FOUND";
    public static final String INVALID_ZONE_TIME_RANGE = "INVALID_ZONE_TIME_RANGE";
    public static final String ZONE_NOT_FOUND = "ZONE_NOT_FOUND";
    public static final String DAY_NOT_FOUND = "DAY_NOT_FOUND";
    public static final String DAY_ALREADY_ASSIGNED = "DAY_ALREADY_ASSIGNED";


    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";

    // AI / Decomposition
    public static final String GOAL_DECOMPOSITION_SESSION_NOT_FOUND = "GOAL_DECOMPOSITION_SESSION_NOT_FOUND";
    public static final String INVALID_DECOMPOSITION = "INVALID_DECOMPOSITION";
    public static final String AI_UNAVAILABLE = "AI_UNAVAILABLE";

    // Not Found
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String GOAL_NOT_FOUND = "GOAL_NOT_FOUND";
    public static final String TASK_NOT_FOUND = "TASK_NOT_FOUND";
    public static final String CATEGORY_NOT_FOUND = "CATEGORY_NOT_FOUND";
}
