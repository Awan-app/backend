package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ItemNotOwnedException extends ApplicationException {

    public ItemNotOwnedException(UUID itemId) {
        super(
            "User does not own item with id=" + itemId,
            HttpStatus.CONFLICT.value(),
            ErrorCodes.ITEM_NOT_OWNED
        );
    }
}
