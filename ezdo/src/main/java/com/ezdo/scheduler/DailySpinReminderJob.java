package com.ezdo.scheduler;

import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.UserRepository;
import com.ezdo.service.FcmNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DailySpinReminderJob implements Job {

    private final UserRepository userRepository;
    private final FcmNotificationService fcmNotificationService;

    @Setter
    private String userId;

    @Override
    @Transactional(readOnly = true)
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (userId == null || userId.isBlank()) {
            return;
        }

        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new UserNotFoundException(userUuid));

        if (user.getPreferences() == null) {
            return;
        }

        if (!Boolean.TRUE.equals(user.getPreferences().getNotificationsEnabled())) {
            return;
        }

        try {
            boolean sent = fcmNotificationService.sendDailySpinReminder(user.getId());
            if (sent) {
                log.info("Sent daily spin reminder push notification to user {}", user.getId());
            } else {
                log.warn("Daily spin reminder job ran, but no FCM message sent to user {}", user.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send daily spin reminder to user {}: {}", user.getId(), e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
