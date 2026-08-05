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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySummarySchedulerService {

    private static final String GROUP = "daily-summaries";

    private final Scheduler scheduler;
    private final UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleAllDailySummaries() {
        try {
            deleteOldPollingJob();
            Set<UUID> eligibleUserIds = userRepository.findAllEligibleForDailySummary().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(GROUP))) {
                UUID scheduledUserId = userIdFrom(jobKey);
                if (scheduledUserId == null || !eligibleUserIds.contains(scheduledUserId)) {
                    scheduler.deleteJob(jobKey);
                    log.info("Removed stale daily summary job {}", jobKey);
                }
            }

            userRepository.findAllEligibleForDailySummary().forEach(this::scheduleDailySummary);
        } catch (SchedulerException e) {
            log.error("Failed to initialize daily summary schedules", e);
        }
    }

    public void syncDailySummary(User user) {
        if (shouldSchedule(user)) {
            scheduleDailySummary(user);
        } else {
            cancelDailySummary(user.getId());
        }
    }

    public void scheduleDailySummary(User user) {
        if (!shouldSchedule(user)) {
            cancelDailySummary(user.getId());
            return;
        }

        Preferences preferences = user.getPreferences();
        LocalTime wakeupTime = preferences.getWakeupTime();
        ZoneId zone = resolveZone(preferences);
        JobKey jobKey = jobKey(user.getId());

        try {
            JobDetail jobDetail = JobBuilder.newJob(DailySummaryJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("userId", user.getId().toString())
                    .storeDurably(false)
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(user.getId()))
                    .withSchedule(CronScheduleBuilder
                            .dailyAtHourAndMinute(wakeupTime.getHour(), wakeupTime.getMinute())
                            .inTimeZone(TimeZone.getTimeZone(zone)))
                    .build();

            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(jobDetail, trigger);

            log.info("Scheduled daily summary for user {} at {} {}", user.getId(), wakeupTime, zone);
        } catch (SchedulerException e) {
            log.error("Failed to schedule daily summary for user {}", user.getId(), e);
        }
    }

    public void cancelDailySummary(UUID userId) {
        try {
            JobKey jobKey = jobKey(userId);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Cancelled daily summary job for user {}", userId);
            }
        } catch (SchedulerException e) {
            log.error("Failed to cancel daily summary for user {}", userId, e);
        }
    }

    private boolean shouldSchedule(User user) {
        Preferences preferences = user.getPreferences();
        return preferences != null
                && Boolean.TRUE.equals(preferences.getDailySummaryEnabled())
                && preferences.getWakeupTime() != null;
    }

    private ZoneId resolveZone(Preferences preferences) {
        try {
            String timezone = preferences.getTimezone();
            return timezone != null ? ZoneId.of(timezone) : ZoneId.of("UTC");
        } catch (DateTimeException ex) {
            return ZoneId.of("UTC");
        }
    }

    private void deleteOldPollingJob() throws SchedulerException {
        JobKey oldJobKey = JobKey.jobKey("dailySummaryJob", "notifications");
        if (scheduler.checkExists(oldJobKey)) {
            scheduler.deleteJob(oldJobKey);
            log.info("Removed old 15-minute daily summary polling job");
        }
    }

    private UUID userIdFrom(JobKey jobKey) {
        String name = jobKey.getName();
        if (!name.startsWith("daily-summary-")) return null;
        try {
            return UUID.fromString(name.substring("daily-summary-".length()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private JobKey jobKey(UUID userId) {
        return JobKey.jobKey("daily-summary-" + userId, GROUP);
    }

    private TriggerKey triggerKey(UUID userId) {
        return TriggerKey.triggerKey("daily-summary-trigger-" + userId, GROUP);
    }
}
