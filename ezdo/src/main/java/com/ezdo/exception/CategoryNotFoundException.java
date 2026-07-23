package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CategoryNotFoundException extends ApplicationException {

    public CategoryNotFoundException(UUID id) {
        super(
            "Category with id=" + id + " not found",
            HttpStatus.NOT_FOUND.value(),
            ErrorCodes.CATEGORY_NOT_FOUND
        );
    }
}
