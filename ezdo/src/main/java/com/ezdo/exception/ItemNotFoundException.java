package com.ezdo.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class ItemNotFoundException extends ApplicationException {
    public ItemNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND.value(), "ITEM_NOT_FOUND");
    }

    public ItemNotFoundException(UUID id) {
        super(
            "Item with id=" + id + " not found",
            HttpStatus.NOT_FOUND.value(),
            "ITEM_NOT_FOUND"
        );
    }
}
