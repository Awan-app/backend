package com.ezdo.service;

import com.ezdo.dto.AvailableSlot;
import com.ezdo.dto.ZoneResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ZoneService zoneService;

    private static final LocalTime DEFAULT_WAKEUP = LocalTime.of(7, 0);
    private static final LocalTime DEFAULT_SLEEP = LocalTime.of(22, 0);

    public List<AvailableSlot> getAvailableSlots(UUID userId, LocalDate date) {
        return getAvailableSlotsForRange(userId, date, date).getOrDefault(date, List.of());
    }

    /**
     * Zones and sessions for the whole range are loaded once and indexed by date,
     * rather than re-queried per day. Resolving a fortnight a day at a time used to
     * cost upwards of forty queries; it is now four, which matters because the AI
     * planner asks for a 14-day horizon on every proposal it makes.
     */
    public Map<LocalDate, List<AvailableSlot>> getAvailableSlotsForRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Preferences prefs = user.getPreferences();

        LocalTime wakeup = prefs.getWakeupTime() != null ? prefs.getWakeupTime() : DEFAULT_WAKEUP;
        LocalTime sleep = prefs.getSleepTime() != null ? prefs.getSleepTime() : DEFAULT_SLEEP;

        Map<LocalDate, List<ZoneResponse>> zonesByDate =
                zoneService.getZonesByDateRange(userId, startDate, endDate);

        // Grouped on the session's start date, matching what the per-day query used
        // to select: sessions that START within the day, not ones spilling in from
        // the day before.
        Map<LocalDate, List<Session>> sessionsByDate = sessionRepository
                .findByUserIdAndDateRange(userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(s -> s.getStart().toLocalDate()));

        Map<LocalDate, List<AvailableSlot>> result = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.put(date, computeSlots(
                    date, wakeup, sleep,
                    zonesByDate.getOrDefault(date, List.of()),
                    sessionsByDate.getOrDefault(date, List.of())));
        }
        return result;
    }

    private List<AvailableSlot> computeSlots(LocalDate date,
                                             LocalTime wakeup,
                                             LocalTime sleep,
                                             List<ZoneResponse> dayZones,
                                             List<Session> sessions) {
        List<AvailableSlot> slots = new ArrayList<>();

        LocalDateTime dayWakeup = date.atTime(wakeup);
        LocalDateTime daySleep = date.atTime(sleep);

        if (!dayWakeup.isBefore(daySleep)) return slots;

        List<ZoneResponse> zones = dayZones.stream()
                .sorted(Comparator.comparing(ZoneResponse::startTime))
                .toList();

        LocalDateTime current = dayWakeup;

        for (ZoneResponse zone : zones) {
            LocalDateTime zoneStart = date.atTime(zone.startTime());
            LocalDateTime zoneEnd = date.atTime(zone.endTime());

            if (zoneStart.isBefore(dayWakeup)) zoneStart = dayWakeup;
            if (zoneEnd.isAfter(daySleep)) zoneEnd = daySleep;
            if (!zoneStart.isBefore(zoneEnd)) continue;

            if (current.isBefore(zoneStart)) {
                slots.add(new AvailableSlot(
                        current,
                        zoneStart,
                        null,
                        null,
                        null,
                        null
                ));
            }

            current = current.isAfter(zoneStart) ? current : zoneStart;

            LocalDateTime slotStart = current;
            for (Session session : sessions) {
                LocalDateTime sesStart = session.getStart();
                LocalDateTime sesEnd = session.getEnd();

                if (sesEnd.isBefore(zoneStart) || sesStart.isAfter(zoneEnd)) continue;

                LocalDateTime overlapStart = sesStart.isAfter(zoneStart) ? sesStart : zoneStart;
                LocalDateTime overlapEnd = sesEnd.isBefore(zoneEnd) ? sesEnd : zoneEnd;

                if (slotStart.isBefore(overlapStart)) {
                    slots.add(new AvailableSlot(
                            slotStart,
                            overlapStart,
                            zone.id(),
                            zone.name(),
                            zone.color(),
                            zone.category()
                    ));
                }
                slotStart = slotStart.isAfter(overlapEnd) ? slotStart : overlapEnd;
            }

            if (slotStart.isBefore(zoneEnd)) {
                slots.add(new AvailableSlot(
                        slotStart,
                        zoneEnd,
                        zone.id(),
                        zone.name(),
                        zone.color(),
                        zone.category()
                ));
            }

            current = current.isAfter(zoneEnd) ? current : zoneEnd;
        }

        if (current.isBefore(daySleep)) {
            slots.add(new AvailableSlot(
                    current,
                    daySleep,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return slots;
    }
}
