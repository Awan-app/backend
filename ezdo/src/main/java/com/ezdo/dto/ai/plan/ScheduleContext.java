package com.ezdo.dto.ai.plan;

import com.ezdo.entity.SessionStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * The user's real calendar over the planning horizon, as the planning model needs to
 * see it: when they are free, and what a proposed session would collide with if the
 * week leaves no free time.
 *
 * <p>Booked items carry more than their times because the model is asked to pick the
 * <em>least costly</em> conflict when one is unavoidable — a ranking it cannot make
 * from bare busy intervals. Every day in the horizon is present, including empty
 * ones: an absent day reads to a model as unknown rather than free.
 */
public record ScheduleContext(
    List<ScheduleDay> days
) {

    public record ScheduleDay(
        LocalDate date,
        DayOfWeek dayOfWeek,
        List<FreeSlot> free,
        List<BookedItem> booked
    ) {}

    /**
     * A gap the user could be scheduled into. Zone and category are null for time
     * outside any zone.
     *
     * <p>{@code zoneId} is never rendered into the prompt — the model reasons about
     * the zone's name and category, and echoing a UUID back would only invite
     * hallucination. It is carried here so the server can attach the right zone to a
     * session once the model has chosen a time.
     */
    public record FreeSlot(
        LocalTime start,
        LocalTime end,
        UUID zoneId,
        String zoneName,
        String categoryName
    ) {}

    /** An existing session, with what it would cost to overlap it. */
    public record BookedItem(
        LocalTime start,
        LocalTime end,
        String taskTitle,
        boolean locked,
        boolean mandatory,
        Integer points,
        SessionStatus status
    ) {}
}
