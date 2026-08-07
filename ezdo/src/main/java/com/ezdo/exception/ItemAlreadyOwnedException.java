package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ItemAlreadyOwnedException extends ApplicationException {
    
    public ItemAlreadyOwnedException(UUID itemId) {
        super(
            "User already owns item with id=" + itemId,
            HttpStatus.CONFLICT.value(),
            ErrorCodes.ITEM_ALREADY_OWNED
        );
    }
}
