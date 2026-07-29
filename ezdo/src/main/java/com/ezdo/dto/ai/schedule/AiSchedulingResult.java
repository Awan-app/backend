package com.ezdo.dto.ai.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiSchedulingResult(
    List<ProposedSession> sessions,
    List<FailedTask> failedTasks
) {}
