package com.ezdo.dto.ai.schedule;

public record FailedTask(
    String taskId,
//    FailureReason reason,
    String message
) {}
