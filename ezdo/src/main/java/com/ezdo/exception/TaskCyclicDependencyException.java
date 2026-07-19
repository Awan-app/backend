package com.ezdo.exception;

import org.springframework.http.HttpStatus;

public class TaskCyclicDependencyException extends ApplicationException {

    public TaskCyclicDependencyException() {
        this("Dependency cycle detected in draft tasks");
    }

    public TaskCyclicDependencyException(String message) {
        super(
            message,
            HttpStatus.BAD_REQUEST.value(),
            ErrorCodes.TASK_CYCLIC_DEPENDENCY
        );
    }
}
