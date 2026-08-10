package com.ezdo.scheduler;

import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySpinSchedulerService {

    private static final String GROUP = "daily-spin";

    private final Scheduler scheduler;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleAllDailySpins() {
        try {
            List<User> eligibleUsers = userRepository.findAllEligibleForDailySpin();
            Set<UUID> eligibleUserIds = eligibleUsers.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(GROUP))) {
                UUID scheduledUserId = userIdFrom(jobKey);
                if (scheduledUserId == null || !eligibleUserIds.contains(scheduledUserId)) {
                    scheduler.deleteJob(jobKey);
                    log.info("Removed stale daily spin job {}", jobKey);
                }
            }

            eligibleUsers.forEach(this::scheduleDailySpin);
        } catch (SchedulerException e) {
            log.error("Failed to initialize daily spin schedules", e);
        }
    }

    public void syncDailySpin(User user) {
        if (!Boolean.TRUE.equals(user.getIsNew())) {
            scheduleDailySpin(user);
        } else {
            cancelDailySpin(user.getId());
        }
    }

    public void scheduleDailySpin(User user) {
        if (Boolean.TRUE.equals(user.getIsNew())) {
            cancelDailySpin(user.getId());
            return;
        }

        Preferences preferences = user.getPreferences();
        ZoneId zone = resolveZone(preferences);
        JobKey jobKey = jobKey(user.getId());

        try {
            JobDetail jobDetail = JobBuilder.newJob(DailySpinReminderJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("userId", user.getId().toString())
                    .storeDurably(false)
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(user.getId()))
                    .withSchedule(CronScheduleBuilder
                            .dailyAtHourAndMinute(0, 0) // Midnight
                            .inTimeZone(TimeZone.getTimeZone(zone)))
                    .build();

            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(jobDetail, trigger);

            log.info("Scheduled daily spin reminder for user {} at 12:00 AM {}", user.getId(), zone);
        } catch (SchedulerException e) {
            log.error("Failed to schedule daily spin reminder for user {}", user.getId(), e);
        }
    }

    public void cancelDailySpin(UUID userId) {
        try {
            JobKey jobKey = jobKey(userId);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Cancelled daily spin job for user {}", userId);
            }
        } catch (SchedulerException e) {
            log.error("Failed to cancel daily spin reminder for user {}", userId, e);
        }
    }

    private ZoneId resolveZone(Preferences preferences) {
        try {
            String timezone = preferences != null ? preferences.getTimezone() : null;
            return timezone != null ? ZoneId.of(timezone) : ZoneId.of("UTC");
        } catch (DateTimeException ex) {
            return ZoneId.of("UTC");
        }
    }

    private UUID userIdFrom(JobKey jobKey) {
        String name = jobKey.getName();
        if (!name.startsWith("daily-spin-")) return null;
        try {
            return UUID.fromString(name.substring("daily-spin-".length()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private JobKey jobKey(UUID userId) {
        return JobKey.jobKey("daily-spin-" + userId, GROUP);
    }

    private TriggerKey triggerKey(UUID userId) {
        return TriggerKey.triggerKey("daily-spin-trigger-" + userId, GROUP);
    }
}
