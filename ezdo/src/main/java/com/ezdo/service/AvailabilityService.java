package com.ezdo.service;

import com.ezdo.dto.AvailableSlot;
import com.ezdo.dto.ZoneResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.util.DateRangeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<AvailableSlot> getAvailableSlots(UUID userId, LocalDate date) {
        return getAvailableSlotsForRange(userId, date, date).getOrDefault(date, List.of());
    }

    /**
     * Zones and sessions for the whole range are loaded once and indexed by date,
     * rather than re-queried per day. Resolving a fortnight a day at a time used to
     * cost upwards of forty queries; it is now four, which matters because the AI
     * planner asks for a 14-day horizon on every proposal it makes.
     */
    @Transactional(readOnly = true)
    public Map<LocalDate, List<AvailableSlot>> getAvailableSlotsForRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        DateRangeValidator.validate(startDate, endDate);
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
                .findOccupyingByUserIdAndDateRange(userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
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

    /**
     * The day is first split into segments — zone intervals clamped to the wake
     * window, and the unzoned gaps between them — and only then are booked
     * sessions subtracted from every segment alike.
     *
     * <p>Carving sessions out per-zone instead would leave the unzoned gaps
     * untouched, so a session in unzoned time (the AI places these) would be
     * reported free, and a session straddling a zone boundary would only have
     * its in-zone half removed.
     */
    private List<AvailableSlot> computeSlots(LocalDate date,
                                             LocalTime wakeup,
                                             LocalTime sleep,
                                             List<ZoneResponse> dayZones,
                                             List<Session> sessions) {
        LocalDateTime dayWakeup = date.atTime(wakeup);
        LocalDateTime daySleep = date.atTime(sleep);

        if (!dayWakeup.isBefore(daySleep)) return List.of();

        List<Session> ordered = sessions.stream()
                .sorted(Comparator.comparing(Session::getStart))
                .toList();

        List<AvailableSlot> slots = new ArrayList<>();
        for (AvailableSlot segment : buildSegments(date, dayWakeup, daySleep, dayZones)) {
            subtractSessions(segment, ordered, slots);
        }
        return slots;
    }

    private List<AvailableSlot> buildSegments(LocalDate date,
                                              LocalDateTime dayWakeup,
                                              LocalDateTime daySleep,
                                              List<ZoneResponse> dayZones) {
        List<AvailableSlot> segments = new ArrayList<>();
        LocalDateTime current = dayWakeup;

        List<ZoneResponse> zones = dayZones.stream()
                .sorted(Comparator.comparing(ZoneResponse::startTime))
                .toList();

        for (ZoneResponse zone : zones) {
            LocalDateTime zoneStart = date.atTime(zone.startTime());
            LocalDateTime zoneEnd = date.atTime(zone.endTime());

            if (zoneStart.isBefore(dayWakeup)) zoneStart = dayWakeup;
            if (zoneEnd.isAfter(daySleep)) zoneEnd = daySleep;
            if (!zoneStart.isBefore(zoneEnd)) continue;

            if (current.isBefore(zoneStart)) {
                segments.add(unzoned(current, zoneStart));
            }
            segments.add(new AvailableSlot(
                    zoneStart, zoneEnd, zone.id(), zone.name(), zone.color(), zone.category()));
            current = zoneEnd;
        }

        if (current.isBefore(daySleep)) {
            segments.add(unzoned(current, daySleep));
        }
        return segments;
    }

    /**
     * Emits the parts of {@code segment} no session covers. A session may leave the
     * segment untouched, clip either end, split it in two, or swallow it entirely —
     * so this contributes anywhere from zero to several slots.
     */
    private void subtractSessions(AvailableSlot segment, List<Session> sessions, List<AvailableSlot> out) {
        LocalDateTime cursor = segment.start();

        for (Session session : sessions) {
            if (!session.getStart().isBefore(segment.end())) break;
            if (!session.getEnd().isAfter(cursor)) continue;

            if (cursor.isBefore(session.getStart())) {
                out.add(within(segment, cursor, session.getStart()));
            }
            cursor = session.getEnd();
            if (!cursor.isBefore(segment.end())) return;
        }

        if (cursor.isBefore(segment.end())) {
            out.add(within(segment, cursor, segment.end()));
        }
    }

    private AvailableSlot unzoned(LocalDateTime start, LocalDateTime end) {
        return new AvailableSlot(start, end, null, null, null, null);
    }

    private AvailableSlot within(AvailableSlot segment, LocalDateTime start, LocalDateTime end) {
        return new AvailableSlot(
                start, end, segment.zoneId(), segment.zoneName(), segment.zoneColor(), segment.category());
    }
}
