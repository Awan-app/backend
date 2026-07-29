package com.ezdo.service;

import com.ezdo.dto.ZoneResponse;
import com.ezdo.dto.ai.*;
import com.ezdo.entity.Goal;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.entity.Task;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiContextBuilder {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final SessionRepository sessionRepository;
    private final ZoneService zoneService;

    public AiSchedulingPayload buildPayload(UUID userId, UUID goalId, Integer horizonDays) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        Preferences prefs = userRepository.findById(userId).orElseThrow().getPreferences();

        ZoneId userZone = prefs.getTimezone() != null
                ? ZoneId.of(prefs.getTimezone())
                : ZoneId.systemDefault(); // fallback — better to make timezone required at signup

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

        List<Session> existingSessions = sessionRepository.findByUserIdAndTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        List<DaySlot> calendar = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<ZoneResponse> dailyZonesDto = zoneService.getZonesByDate(userId, date);
            List<AvailableZone> availableZones = dailyZonesDto.stream()
                    .map(z -> new AvailableZone(
                            z.id().toString(),
                            z.name(),
                            z.startTime(),
                            z.endTime(),
                            Duration.between(z.startTime(), z.endTime()).toMinutes(),
                            z.category() != null ? z.category().name() : null
                    ))
                    .toList();

            LocalDate finalDate = date;
            List<BookedSession> booked = existingSessions.stream()
                    .filter(s -> s.getStart().toLocalDate().equals(finalDate))
                    .map(s -> new BookedSession(s.getStart().toLocalTime(), s.getEnd().toLocalTime()))
                    .toList();

            calendar.add(new DaySlot(date, date.getDayOfWeek().name(), availableZones, booked));
        }

        return new AiSchedulingPayload(
                goal.getTitle(),
                goal.getDescription(),
                startDate,
                endDate,
                now,
                taskItems,
                prefContext,
                calendar
        );
    }
}
