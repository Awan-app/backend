package com.ezdo.scheduler;

import com.ezdo.entity.Session;
import com.ezdo.entity.SessionStatus;
import com.ezdo.entity.User;
import com.ezdo.repository.SessionRepository;
import com.ezdo.service.FcmNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionReminderJob extends QuartzJobBean {

    private final SessionRepository sessionRepository;
    private final FcmNotificationService fcmNotificationService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    // Quartz automatically injects the "sessionId" passed from the SchedulerService here
    @Setter
    private String sessionId;

    @Override
    @Transactional(readOnly = true)
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        if (sessionId == null || sessionId.isBlank()) {
            log.error("SessionReminderJob executed without a valid sessionId in JobDataMap.");
            return;
        }

        UUID sessionUuid = UUID.fromString(sessionId);
        log.info("Executing precise event-driven reminder for Session ID: {}", sessionUuid);

        try {
            // Fresh Lookup Pattern: Avoids stale data if the user/AI updated the task recently
            Session session = sessionRepository.findById(sessionUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionUuid));

            if (session.getStatus() != SessionStatus.SCHEDULED) {
                log.info("Skipping reminder for session {}: status is {}", sessionUuid, session.getStatus());
                return;
            }

            // Safely navigate relationships (Ensure your repository uses JOIN FETCH if lazy-loading throws an exception)
            User user = session.getTask().getGoal().getUser();
            String taskTitle = session.getTask().getTitle();
            String sessionTime = session.getStart().format(TIME_FORMATTER);

            // Fire the multicast push notification
            boolean sent = fcmNotificationService.sendSessionReminderToUser(
                    user.getId(),
                    session.getId(),
                    taskTitle,
                    sessionTime
            );

            if (sent) {
                log.info("Successfully sent reminder for session {} to user {}", sessionUuid, user.getId());
            } else {
                log.warn("Reminder job ran for session {}, but no FCM message was sent to user {}", sessionUuid, user.getId());
            }

        } catch (Exception e) {
            log.error("Failed to execute SessionReminderJob for session {}", sessionUuid, e);
            // Throwing JobExecutionException allows Quartz to handle retry configurations if needed
            throw new JobExecutionException(e);
        }
    }
}
