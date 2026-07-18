package com.ezdo.exception;

public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException() {
        super("User not found.", 404, ErrorCodes.USER_NOT_FOUND);
    }
}
