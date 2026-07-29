package com.ezdo.dto.ai.schedule;

import java.util.UUID;

public record UnscheduledTaskResult(
    UUID taskId,
    String taskTitle,
    String reason,
    String message
) {}
