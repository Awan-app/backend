package com.ezdo.dto.ai;

public record FailedTask(
    String taskId,
    FailureReason reason,
    String message
) {}
