package com.ezdo.dto.ai.decompose;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * The per-user grounding passed into every AI prompt. Carries the current wall-clock
 * moment in the user's own timezone — not just the date — because resolving phrasing
 * like "at 7pm" ("today if that's still ahead, otherwise tomorrow") needs the time.
 */
public record DecompositionUserContext(
    LocalDateTime now,
    String timezone,
    Integer preferredSessionDuration,
    Integer bufferBetweenSessions,
    LocalTime wakeupTime,
    LocalTime sleepTime,
    List<CategoryOption> categories
) {
    public LocalDate today() {
        return now.toLocalDate();
    }

    public DayOfWeek dayOfWeek() {
        return now.getDayOfWeek();
    }

    public record CategoryOption(UUID id, String name) {}
}
