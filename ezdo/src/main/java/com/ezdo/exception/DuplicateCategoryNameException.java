package com.ezdo.exception;

import org.springframework.http.HttpStatus;

public class DuplicateCategoryNameException extends ApplicationException {

    public DuplicateCategoryNameException(String name) {
        super(
            "You already have a category named '" + name + "'",
            HttpStatus.CONFLICT.value(),
            ErrorCodes.CATEGORY_NAME_TAKEN
        );
    }
}
