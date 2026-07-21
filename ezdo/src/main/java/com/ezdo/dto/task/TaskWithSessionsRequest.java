package com.ezdo.dto.task;

import com.ezdo.dto.goal.TaskCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TaskWithSessionsRequest(
    TaskCreateRequest task,

    @Valid
    @Size(max = 50, message = "At most 50 sessions can be created at once")
    List<SessionDraftRequest> sessions
) {
    public TaskWithSessionsRequest {
        if (sessions == null) sessions = List.of();
    }
}
