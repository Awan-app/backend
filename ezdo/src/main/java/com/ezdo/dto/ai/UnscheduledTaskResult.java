package com.ezdo.dto.ai;

import java.util.UUID;

public record UnscheduledTaskResult(
    UUID taskId,
    String taskTitle,
    String reason,
    String message
) {}
