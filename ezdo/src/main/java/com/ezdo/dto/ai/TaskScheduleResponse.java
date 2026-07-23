package com.ezdo.dto.ai;

import java.util.List;
import java.util.UUID;

public record TaskScheduleResponse(
    UUID taskId,
    List<ScheduledSessionResult> scheduledSessions,
    List<UnscheduledTaskResult> unscheduledTasks
) {}