package com.ezdo.dto.ai.schedule;

import java.time.LocalDate;
import java.util.List;

public record DaySlot(
    LocalDate date,
    String dayOfWeek,
    List<AvailableZone> zones,
    List<BookedSession> bookedSessions
) {}
