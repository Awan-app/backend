package com.ezdo.service.ai;

import com.ezdo.dto.AvailableSlot;
import com.ezdo.dto.ai.AiUserPreferencesContext;
import com.ezdo.dto.ai.plan.ScheduleContext;
import com.ezdo.entity.Session;
import com.ezdo.entity.Task;
import com.ezdo.repository.SessionRepository;
import com.ezdo.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assembles the user's real calendar over the planning horizon for
 * {@code TaskPlanningService}, so the model proposes times the user actually has
 * free instead of inventing them.
 *
 * <p>Read-only transactional because the booked sessions are loaded with the task
 * fetch-joined — {@link AvailabilityService} deliberately has no transaction, since
 * it only ever touches session start/end times.
 */
@Service
@RequiredArgsConstructor
public class ScheduleContextService {

    private final AvailabilityService availabilityService;
    private final SessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public ScheduleContext buildFor(UUID userId, AiUserPreferencesContext context, int horizonDays) {
        LocalDate start = context.today();
        LocalDate end = start.plusDays(Math.max(horizonDays, 1) - 1);

        Map<LocalDate, List<AvailableSlot>> freeByDate =
            availabilityService.getAvailableSlotsForRange(userId, start, end);

        Map<LocalDate, List<Session>> bookedByDate = sessionRepository
            .findByUserIdAndDateRangeWithTask(userId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
            .stream()
            .collect(Collectors.groupingBy(s -> s.getStart().toLocalDate()));

        List<ScheduleContext.ScheduleDay> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            days.add(new ScheduleContext.ScheduleDay(
                date,
                date.getDayOfWeek(),
                toFreeSlots(freeByDate.getOrDefault(date, List.of()), date, context.now()),
                toBookedItems(bookedByDate.getOrDefault(date, List.of()))
            ));
        }
        return new ScheduleContext(days);
    }

    /**
     * {@link AvailabilityService} starts every day at the user's wake-up time with no
     * regard for the clock, so at 15:00 it still reports 09:00-11:00 as free. Offering
     * that to the model invites a proposal in the past, which its own rules forbid —
     * so today's elapsed time is dropped here. The trim lives in this service rather
     * than in {@code AvailabilityService} because that service also backs
     * {@code GET /api/v1/availability/*}, where showing the whole day is correct.
     */
    private List<ScheduleContext.FreeSlot> toFreeSlots(List<AvailableSlot> slots,
                                                       LocalDate date,
                                                       LocalDateTime now) {
        boolean isToday = date.equals(now.toLocalDate());
        List<ScheduleContext.FreeSlot> free = new ArrayList<>();

        for (AvailableSlot slot : slots) {
            LocalDateTime slotStart = slot.start();
            if (isToday) {
                if (!slot.end().isAfter(now)) {
                    continue;
                }
                if (slotStart.isBefore(now)) {
                    slotStart = now;
                }
            }
            free.add(new ScheduleContext.FreeSlot(
                slotStart.toLocalTime(),
                slot.end().toLocalTime(),
                slot.zoneId(),
                slot.zoneName(),
                slot.category() != null ? slot.category().name() : null
            ));
        }
        return free;
    }

    private List<ScheduleContext.BookedItem> toBookedItems(List<Session> sessions) {
        return sessions.stream()
            .map(session -> {
                Task task = session.getTask();
                return new ScheduleContext.BookedItem(
                    session.getStart().toLocalTime(),
                    session.getEnd().toLocalTime(),
                    task.getTitle(),
                    session.isLocked(),
                    Boolean.TRUE.equals(task.getMandatory()),
                    task.getEstimatedPoints(),
                    session.getStatus()
                );
            })
            .toList();
    }
}
