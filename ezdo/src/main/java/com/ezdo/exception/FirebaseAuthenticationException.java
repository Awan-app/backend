package com.ezdo.exception;

public class FirebaseAuthenticationException extends ApplicationException {

    public FirebaseAuthenticationException(String message, String errorCode) {
        super(message, 401, errorCode);
    }

    public FirebaseAuthenticationException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }
}
