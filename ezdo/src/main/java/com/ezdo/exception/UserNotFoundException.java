package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException() {
        super("User not found.", 404, ErrorCodes.USER_NOT_FOUND);
    }

    public UserNotFoundException(UUID id) {
        super(
            "User with id=" + id + " not found",
            HttpStatus.NOT_FOUND.value(),
            ErrorCodes.USER_NOT_FOUND
        );
    }
}
