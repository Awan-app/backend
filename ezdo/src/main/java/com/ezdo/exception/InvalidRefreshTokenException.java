package com.ezdo.exception;

public class InvalidRefreshTokenException extends ApplicationException {

    public InvalidRefreshTokenException(String errorCode) {
        super("Invalid or expired refresh token.", 401, errorCode);
    }

    public InvalidRefreshTokenException(String message, String errorCode) {
        super(message, 401, errorCode);
    }
}
