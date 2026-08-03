package com.ezdo.dto.ai.schedule;

import com.ezdo.entity.SessionStatus;

import java.time.LocalTime;

/**
 * A rich representation of an existing booked session sent to the AI so it can
 * apply cost-ranked conflict resolution when no free time is available.
 *
 * <p>Carries the same cost signals as {@link com.ezdo.dto.ai.plan.ScheduleContext.BookedItem}
 * used by the single-task planning flow, ensuring identical conflict-ranking
 * behaviour in goal scheduling.
 */
public record BookedSessionItem(
    LocalTime start,
    LocalTime end,
    String taskTitle,
    boolean locked,
    boolean mandatory,
    Integer points,
    SessionStatus status
) {}
