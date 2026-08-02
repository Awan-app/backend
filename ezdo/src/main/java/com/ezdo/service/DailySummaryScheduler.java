package com.ezdo.service;

import com.ezdo.dto.email.DeadlineSummary;
import com.ezdo.dto.email.GoalSummary;
import com.ezdo.dto.email.MorningSummaryEmail;
import com.ezdo.dto.email.SessionSummary;
import com.ezdo.entity.Goal;
import com.ezdo.entity.GoalStatus;
import com.ezdo.entity.Session;
import com.ezdo.entity.TaskStatus;
import com.ezdo.entity.User;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailySummaryScheduler {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final GoalRepository goalRepository;
    private final EmailService emailService;

    private static final LocalTime DEFAULT_WAKEUP = LocalTime.of(7, 0);

    // @Scheduled(cron = "0 */15 * * * *")
    @Transactional(readOnly = true)
    public void dispatchWindow() {
        List<User> users = userRepository.findAllEligibleForDailySummary();

        for (User user : users) {
            ZoneId zone = resolveZone(user);
            ZonedDateTime nowLocal = ZonedDateTime.now(zone);
            LocalTime wakeup = resolveWakeupTime(user);

            if (!isWithinWindow(nowLocal.toLocalTime(), wakeup)) {
                continue;
            }
            try {
                sendFor(user, nowLocal.toLocalDate());
            } catch (Exception e) {
                log.error("Failed to send daily summary to user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
    }

    private void sendFor(User user, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<Session> sessions = sessionRepository.findByUserIdAndDate(user.getId(), dayStart, dayEnd);

        List<SessionSummary> sessionSummaries = sessions.stream()
                .sorted(Comparator.comparing(Session::getStart))
                .map(s -> new SessionSummary(
                        s.getTask().getTitle(),
                        s.getStart().toLocalTime(),
                        s.getEnd().toLocalTime()))
                .toList();

        int totalFocusMinutes = sessions.stream()
                .mapToInt(s -> (int) ChronoUnit.MINUTES.between(s.getStart(), s.getEnd()))
                .sum();

        List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(
                user.getId(), GoalStatus.ACTIVE, Pageable.unpaged()).getContent();

        List<GoalSummary> goalSummaries = activeGoals.stream()
                .filter(g -> !Boolean.TRUE.equals(g.getInbox()))
                .map(g -> {
                    long total = g.getTasks().size();
                    long completed = g.getTasks().stream()
                            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                            .count();
                    int progress = total == 0 ? 0 : (int) (completed * 100 / total);
                    return new GoalSummary(g.getTitle(), progress);
                })
                .toList();

        List<DeadlineSummary> deadlineSummaries = activeGoals.stream()
                .filter(g -> !Boolean.TRUE.equals(g.getInbox()))
                .filter(g -> g.getTargetDate() != null)
                .sorted(Comparator.comparing(Goal::getTargetDate))
                .map(g -> new DeadlineSummary(
                        g.getTitle(),
                        g.getTargetDate(),
                        ChronoUnit.DAYS.between(date, g.getTargetDate())))
                .toList();

        MorningSummaryEmail email = new MorningSummaryEmail(
                user.getFirstName(),
                date,
                sessionSummaries,
                goalSummaries,
                deadlineSummaries,
                totalFocusMinutes);

        emailService.sendDailySummaryEmail(user.getEmail(), email);
    }

    private boolean isWithinWindow(LocalTime now, LocalTime wakeup) {
        int nowBucket = (now.getHour() * 60 + now.getMinute()) / 15;
        int wakeupBucket = (wakeup.getHour() * 60 + wakeup.getMinute()) / 15;
        return nowBucket == wakeupBucket;
    }

    private LocalTime resolveWakeupTime(User user) {
        LocalTime w = user.getPreferences().getWakeupTime();
        return w != null ? w : DEFAULT_WAKEUP;
    }

    private ZoneId resolveZone(User user) {
        String tz = user.getPreferences().getTimezone();
        return tz != null ? ZoneId.of(tz) : ZoneId.of("UTC");
    }
}
