package com.ezdo.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddSessionsRequest(
    @NotEmpty(message = "At least one session is required")
    @Valid
    @Size(max = 50, message = "At most 50 sessions can be added at once")
    List<SessionDraftRequest> sessions
) {
}
