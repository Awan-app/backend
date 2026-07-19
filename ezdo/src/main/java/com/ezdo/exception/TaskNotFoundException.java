package com.ezdo.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TaskNotFoundException extends ApplicationException {

    public TaskNotFoundException(UUID taskId) {
        super(
            "Task with id=" + taskId + " not found",
            HttpStatus.NOT_FOUND.value(),
            ErrorCodes.TASK_NOT_FOUND
        );
    }
}
