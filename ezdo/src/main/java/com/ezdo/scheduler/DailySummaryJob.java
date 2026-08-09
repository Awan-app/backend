package com.ezdo.scheduler;

import com.ezdo.dto.email.DeadlineSummary;
import com.ezdo.dto.email.GoalSummary;
import com.ezdo.dto.email.MorningSummaryEmail;
import com.ezdo.dto.email.SessionSummary;
import com.ezdo.entity.Goal;
import com.ezdo.entity.GoalStatus;
import com.ezdo.entity.Session;
import com.ezdo.entity.Task;
import com.ezdo.entity.User;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.service.EmailService;
import com.ezdo.service.FcmNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DailySummaryJob implements Job {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final GoalRepository goalRepository;
    private final EmailService emailService;
    private final FcmNotificationService fcmNotificationService;

    @Setter
    private String userId;

    @Override
    @Transactional(readOnly = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (userId == null || userId.isBlank()) {
            log.error("DailySummaryJob executed without a valid userId in JobDataMap.");
            return;
        }

        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new JobExecutionException("User not found: " + userUuid));

        if (user.getPreferences() == null || !Boolean.TRUE.equals(user.getPreferences().getDailySummaryEnabled())) {
            log.info("Skipping daily summary for user {} because daily summaries are disabled", userUuid);
            return;
        }

        try {
            ZoneId zone = resolveZone(user);
            ZonedDateTime nowLocal = ZonedDateTime.now(zone);
            sendFor(user, nowLocal.toLocalDate());
        } catch (Exception e) {
            log.error("Failed to send daily summary to user {}: {}", user.getId(), e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }

    private void sendFor(User user, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<Session> sessions = sessionRepository
                .findByUserIdAndDateRangeWithTask(user.getId(), dayStart, dayEnd);

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
                            .filter(Task::isCompleted)
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

        // Send email
        emailService.sendDailySummaryEmail(user.getEmail(), email);
        log.info("Sent daily summary email to user {}", user.getId());

        // Send push notification to all user devices
        fcmNotificationService.sendDailySummaryToUser(
                user.getId(),
                user.getFirstName(),
                sessions.size()
        );
    }

    private ZoneId resolveZone(User user) {
        String tz = user.getPreferences().getTimezone();
        return tz != null ? ZoneId.of(tz) : ZoneId.of("UTC");
    }
}
