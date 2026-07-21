package com.ezdo.dto.task;

import com.ezdo.dto.SessionResponse;
import com.ezdo.dto.goal.TaskInfoResponse;

import java.util.List;

public record TaskWithSessionsResponse(
    TaskInfoResponse task,
    List<SessionResponse> sessions
) {
}
