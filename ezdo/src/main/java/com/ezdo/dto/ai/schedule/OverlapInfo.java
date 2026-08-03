package com.ezdo.dto.ai.schedule;

import java.time.LocalDateTime;

/**
 * Details about the existing session that would be overlapped by a
 * {@link SuggestionType#OVERLAP} suggestion. Sent to the client so the user
 * can decide whether the trade-off is acceptable before confirming.
 */
public record OverlapInfo(
    String taskTitle,
    LocalDateTime start,
    LocalDateTime end,
    boolean mandatory,
    Integer points
) {}
