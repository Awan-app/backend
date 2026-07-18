package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException(UUID id) {
        super(
            "User with id=" + id + " not found",
            HttpStatus.NOT_FOUND.value(),
            ErrorCodes.USER_NOT_FOUND
        );
    }
}
