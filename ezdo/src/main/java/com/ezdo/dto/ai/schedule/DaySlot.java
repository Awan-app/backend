package com.ezdo.dto.ai.schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a single day in the scheduling horizon as seen by the AI.
 *
 * <p>{@code freeSlots} are the actual schedulable gaps after existing sessions
 * have already been subtracted from zone time — the AI never needs to compute
 * availability itself.
 *
 * <p>{@code bookedSessions} carry full cost-signal context (locked, mandatory,
 * points, status) so the AI can rank conflicts when no free slot is available.
 */
public record DaySlot(
    LocalDate date,
    String dayOfWeek,
    List<FreeSlotItem> freeSlots,
    List<BookedSessionItem> bookedSessions
) {}
