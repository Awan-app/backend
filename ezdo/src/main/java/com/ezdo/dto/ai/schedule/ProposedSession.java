package com.ezdo.dto.ai.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * A session proposed by the AI in its JSON response.
 *
 * <p>When {@code suggestionType} is {@code null} the session is a normal
 * in-zone placement.  When it is {@code "NO_ZONE"} or {@code "OVERLAP"} the
 * session is a suggestion that requires user confirmation before it is saved.
 *
 * <p>{@code zoneId} is nullable — the AI omits it for no-zone suggestions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProposedSession(
    String taskId,
    String zoneId,             // null when suggestionType == "NO_ZONE"
    LocalDateTime startTime,
    LocalDateTime endTime,
    String suggestionType,     // null | "NO_ZONE" | "OVERLAP"
    String reason              // explanation, required when suggestionType != null
) {}
