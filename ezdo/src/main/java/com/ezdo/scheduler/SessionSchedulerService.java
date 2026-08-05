package com.ezdo.scheduler;

import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.entity.User;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;


//
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSchedulerService {

    private final Scheduler scheduler;
    private final UserRepository userRepository;
    private static final int REMINDER_MINUTES_BEFORE = 10;

    /**
     * Schedule (or replace) the reminder job for a session, resolving the user's
     * timezone from their stored preferences. Safe to call on both create and
     * reschedule — it overwrites any existing job.
     */
    public void scheduleReminder(Session session, UUID userId) {
        scheduleReminder(session, resolveUserZone(userId));
    }

    /**
     * Schedule (or replace) the reminder job for a session.
     * Safe to call on both create and reschedule — it overwrites any existing job.
     */
    public void scheduleReminder(Session session, ZoneId userZone) {
        ZonedDateTime reminderTime = session.getStart()
                .atZone(userZone)
                .minusMinutes(REMINDER_MINUTES_BEFORE);

        // Don't schedule reminders for moments that have already passed
        if (reminderTime.isBefore(ZonedDateTime.now(userZone))) {
            log.info("Reminder time for session {} is in the past; skipping schedule", session.getId());
            cancelReminder(session.getId()); // clean up any stale job from a prior time, just in case
            return;
        }

        JobKey jobKey = jobKey(session.getId());

        try {
            JobDetail jobDetail = JobBuilder.newJob(SessionReminderJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("sessionId", session.getId().toString())
                    .storeDurably(false)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(session.getId()))
                    .startAt(Date.from(reminderTime.toInstant()))
                    .build();

            // replaceJob semantics: delete existing job (if any) then schedule fresh
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(jobDetail, trigger);

            log.info("Scheduled reminder for session {} at {}", session.getId(), reminderTime);
        } catch (SchedulerException e) {
            // Don't let a scheduling failure block session creation/update itself
            log.error("Failed to schedule reminder for session {}", session.getId(), e);
        }
    }

    /**
     * Remove the reminder job for a session — call on cancellation or deletion.
     */
    public void cancelReminder(UUID sessionId) {
        try {
            JobKey jobKey = jobKey(sessionId);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Cancelled reminder job for session {}", sessionId);
            }
        } catch (SchedulerException e) {
            log.error("Failed to cancel reminder for session {}", sessionId, e);
        }
    }

    private JobKey jobKey(UUID sessionId) {
        return JobKey.jobKey("session-reminder-" + sessionId, "reminders");
    }

    private TriggerKey triggerKey(UUID sessionId) {
        return TriggerKey.triggerKey("session-reminder-trigger-" + sessionId, "reminders");
    }

    private ZoneId resolveUserZone(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getPreferences)
                .filter(p -> p != null && p.getTimezone() != null)
                .map(Preferences::getTimezone)
                .map(ZoneId::of)
                .orElse(ZoneId.of("UTC"));
    }
}