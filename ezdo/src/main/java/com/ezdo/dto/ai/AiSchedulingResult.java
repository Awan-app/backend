package com.ezdo.dto.ai;

import java.util.List;

public record AiSchedulingResult(
    String _thinkingProcess,
    List<ProposedSession> sessions,
    List<FailedTask> failedTasks
) {}
