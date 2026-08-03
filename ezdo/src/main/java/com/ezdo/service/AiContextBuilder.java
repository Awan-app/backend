package com.ezdo.service;

import com.ezdo.dto.AvailableSlot;
import com.ezdo.dto.ai.schedule.*;
import com.ezdo.entity.Goal;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.exception.GoalNotFoundException;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the {@link AiSchedulingPayload} sent to the AI for goal scheduling.
 *
 * <h3>What changed from the original implementation</h3>
 * <ul>
 *   <li>The calendar now carries <em>true free gaps</em> ({@link FreeSlotItem})
 *       computed by {@link AvailabilityService} — existing sessions are already
 *       subtracted, so the AI never needs to compute availability itself.</li>
 *   <li>Booked sessions now carry full cost-signal context ({@link BookedSessionItem}):
 *       task title, locked flag, mandatory flag, estimated points, and status —
 *       the same signals the single-task planner uses for conflict ranking.</li>
 *   <li>Past time on today's date is trimmed before being offered to the AI,
 *       preventing proposals that would start in the past.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AiContextBuilder {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final SessionRepository sessionRepository;
    private final AvailabilityService availabilityService;

    public AiSchedulingPayload buildPayload(UUID userId, UUID goalId, Integer horizonDays) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        Preferences prefs = userRepository.findById(userId).orElseThrow().getPreferences();

        ZoneId userZone = prefs.getTimezone() != null
                ? ZoneId.of(prefs.getTimezone())
                : ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(userZone);
        LocalDate startDate = now.toLocalDate();

        int days = horizonDays != null ? horizonDays : 14;
        LocalDate maxEndDate = startDate.plusDays(days);

        LocalDate endDate = goal.getTargetDate();
        if (endDate == null || endDate.isAfter(maxEndDate)) {
            endDate = maxEndDate;
        } else if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }

        UserPreferencesContext prefContext = new UserPreferencesContext(
                prefs.getPreferredSessionDuration() != null ? prefs.getPreferredSessionDuration() : 60,
                prefs.getBufferBetweenSessions() != null ? prefs.getBufferBetweenSessions() : 0,
                prefs.getWakeupTime(),
                prefs.getSleepTime(),
                prefs.getSchedulingType() != null ? prefs.getSchedulingType().name() : "BALANCED"
        );

        List<AiTaskItem> taskItems = goal.getTasks().stream()
                .map(t -> new AiTaskItem(
                        t.getId().toString(),
                        t.getTitle(),
                        t.getDescription(),
                        t.getEstimatedDuration() != null ? t.getEstimatedDuration() : 60,
                        t.getEstimatedPoints() != null ? t.getEstimatedPoints() : 0,
                        Boolean.TRUE.equals(t.getMandatory()),
                        Boolean.TRUE.equals(t.getAllowTaskSplitting()),
                        t.getDependsOn().stream().map(d -> d.getId().toString()).toList(),
                        t.getCategory() != null ? t.getCategory().getId().toString() : null,
                        t.getCategory() != null ? t.getCategory().getName() : null
                ))
                .toList();

        // ── free slots ──────────────────────────────────────────────────────
        // AvailabilityService already subtracts booked sessions from zone time,
        // giving us the true schedulable gaps for the entire range in one call.
        Map<LocalDate, List<AvailableSlot>> freeByDate =
                availabilityService.getAvailableSlotsForRange(userId, startDate, endDate);

        // ── booked sessions with task context ───────────────────────────────
        // fetch-joined query so task title / flags are available without N+1.
        LocalDate finalEndDate = endDate;
        Map<LocalDate, List<Session>> sessionsByDate = sessionRepository
                .findByUserIdAndDateRangeWithTask(
                        userId,
                        startDate.atStartOfDay(),
                        finalEndDate.plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(s -> s.getStart().toLocalDate()));

        // ── assemble calendar ────────────────────────────────────────────────
        List<DaySlot> calendar = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(finalEndDate); date = date.plusDays(1)) {
            boolean isToday = date.equals(now.toLocalDate());

            List<FreeSlotItem> freeSlots = toFreeSlots(
                    freeByDate.getOrDefault(date, List.of()), date, now, isToday);

            List<BookedSessionItem> booked = toBookedItems(
                    sessionsByDate.getOrDefault(date, List.of()));

            calendar.add(new DaySlot(date, date.getDayOfWeek().name(), freeSlots, booked));
        }

        return new AiSchedulingPayload(
                goal.getTitle(),
                goal.getDescription(),
                startDate,
                finalEndDate,
                now,
                taskItems,
                prefContext,
                calendar
        );
    }
    /**
     * Converts {@link AvailableSlot} records to {@link FreeSlotItem} records,
     * trimming any portion that lies in the past on today's date.
     */
    private List<FreeSlotItem> toFreeSlots(List<AvailableSlot> slots,
                                            LocalDate date,
                                            LocalDateTime now,
                                            boolean isToday) {
        List<FreeSlotItem> result = new ArrayList<>();
        for (AvailableSlot slot : slots) {
            LocalDateTime slotStart = slot.start();
            LocalDateTime slotEnd   = slot.end();

            if (isToday) {
                // Drop slots entirely in the past
                if (!slotEnd.isAfter(now)) continue;
                // Trim the leading portion that has already passed
                if (slotStart.isBefore(now)) slotStart = now;
            }

            long durationMinutes = Duration.between(slotStart, slotEnd).toMinutes();
            if (durationMinutes <= 0) continue;

            result.add(new FreeSlotItem(
                    slotStart.toLocalTime(),
                    slotEnd.toLocalTime(),
                    durationMinutes,
                    slot.zoneId(),                                          // @JsonIgnore — server only
                    slot.zoneName(),                                        // null = unzoned gap
                    slot.category() != null ? slot.category().name() : null // null = no category
            ));
        }
        return result;
    }

    private List<BookedSessionItem> toBookedItems(List<Session> sessions) {
        return sessions.stream()
                .map(s -> new BookedSessionItem(
                        s.getStart().toLocalTime(),
                        s.getEnd().toLocalTime(),
                        s.getTask().getTitle(),
                        s.isLocked(),
                        Boolean.TRUE.equals(s.getTask().getMandatory()),
                        s.getTask().getEstimatedPoints(),
                        s.getStatus()
                ))
                .toList();
    }
}
