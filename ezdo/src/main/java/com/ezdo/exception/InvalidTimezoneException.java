package com.ezdo.exception;

public class InvalidTimezoneException extends ApplicationException {
    public InvalidTimezoneException() {
        super("The provided timezone is invalid.", 400, ErrorCodes.INVALID_TIMEZONE);
    }
}
