package com.ezdo.dto.ai.schedule;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single session to persist as part of a schedule-confirmation call.
 *
 * <p>The client constructs this from any combination of
 * {@link ProposedSessionResult} (confirmed in-zone sessions) and accepted
 * {@link SuggestedSession} items returned by the scheduling response.
 *
 * <p>{@code zoneId} may be {@code null} when the user is confirming a
 * {@link SuggestionType#NO_ZONE} suggestion.
 */
public record ConfirmSessionItem(
    @NotNull UUID taskId,
    UUID zoneId,                 // nullable — null for unzoned suggestions
    @NotNull LocalDateTime start,
    @NotNull LocalDateTime end
) {}
