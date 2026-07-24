package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class GoalDecompositionSessionNotFoundException extends ApplicationException {

    public GoalDecompositionSessionNotFoundException(UUID id) {
        super(
            "Goal decomposition session with id=" + id + " not found",
            HttpStatus.NOT_FOUND.value(),
            ErrorCodes.GOAL_DECOMPOSITION_SESSION_NOT_FOUND
        );
    }
}
