package com.ezdo.dto.ai.schedule;

import java.util.List;
import java.util.UUID;

public record GoalScheduleResponse(
    UUID goalId,
    List<ScheduledSessionResult> scheduledSessions,
    List<UnscheduledTaskResult> unscheduledTasks
) {}
