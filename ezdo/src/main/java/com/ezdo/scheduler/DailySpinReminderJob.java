package com.ezdo.scheduler;

import com.ezdo.entity.User;
import com.ezdo.repository.UserRepository;
import com.ezdo.service.FcmNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DailySpinReminderJob extends QuartzJobBean {

    private final UserRepository userRepository;
    private final FcmNotificationService fcmNotificationService;

    @Setter
    private String userId;

    @Override
    @Transactional(readOnly = true)
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        if (userId == null || userId.isBlank()) {
            log.error("DailySpinReminderJob executed without a valid userId in JobDataMap.");
            return;
        }

        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid).orElse(null);

        if (user == null) {
            log.warn("User {} no longer exists; skipping daily spin reminder", userUuid);
            return;
        }

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
