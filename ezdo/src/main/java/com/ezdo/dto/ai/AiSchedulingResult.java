package com.ezdo.dto.ai;

import java.util.List;

public record AiSchedulingResult(
    List<ProposedSession> sessions,
    List<FailedTask> failedTasks
) {}
