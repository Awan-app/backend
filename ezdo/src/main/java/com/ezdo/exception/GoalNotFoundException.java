package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class GoalNotFoundException extends ApplicationException {

    public GoalNotFoundException(UUID id) {
        super(
            "Goal with id=" + id + " not found",
            HttpStatus.NOT_FOUND.value(),
            ErrorCodes.GOAL_NOT_FOUND
        );
    }
}
