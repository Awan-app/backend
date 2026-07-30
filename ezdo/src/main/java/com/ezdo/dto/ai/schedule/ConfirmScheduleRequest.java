package com.ezdo.dto.ai.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/ai/schedule/confirm}.
 *
 * <p>The client selects which sessions from the scheduling response to persist
 * (some or all of {@code proposedSessions} and any accepted suggestions) and
 * sends them here.  The server validates ownership, resolves zones, and saves
 * each one as a {@link com.ezdo.entity.Session}.
 */
public record ConfirmScheduleRequest(
    @NotNull UUID goalId,
    @NotEmpty @Valid List<ConfirmSessionItem> sessions
) {}
