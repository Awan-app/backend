package com.ezdo.service;

import com.ezdo.entity.DeviceToken;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    // 1. Inject the Service (for cleanup) instead of the Repository
    private final DeviceTokenService deviceTokenService;

    // 2. Inject Optional<FirebaseMessaging> to respect graceful degradation
    private final Optional<FirebaseMessaging> firebaseMessaging;

    /**
     * Send session reminder to all user's devices using Multicast (1 network call)
     */
    public boolean sendSessionReminderToUser(UUID userId, UUID sessionId, String taskTitle, String sessionTime) {
        List<String> tokens = getUserTokens(userId);
        if (tokens.isEmpty()) return false;

        Map<String, String> data = new HashMap<>();
        data.put("type", "SESSION_REMINDER");
        data.put("sessionId", sessionId.toString());
        data.put("taskTitle", taskTitle);
        data.put("sessionTime", sessionTime);

        String title = "Session Starting Soon";
        String body = "Your session \"" + taskTitle + "\" starts at " + sessionTime;

        return sendMulticastNotification(tokens, title, body, data, AndroidConfig.Priority.HIGH, "session_reminders");
    }

    /**
     * Send daily summary to all user's devices using Multicast
     */
    public boolean sendDailySummaryToUser(UUID userId, String userName, int sessionCount) {
        List<String> tokens = getUserTokens(userId);
        if (tokens.isEmpty()) return false;

        Map<String, String> data = new HashMap<>();
        data.put("type", "DAILY_SUMMARY");
        data.put("sessionCount", String.valueOf(sessionCount));

        String title = "Good morning, " + userName + "!";
        String body = sessionCount > 0
                ? "You have " + sessionCount + " session(s) scheduled for today"
                : "Your day is clear today";

        return sendMulticastNotification(tokens, title, body, data, AndroidConfig.Priority.NORMAL, "daily_summary");
    }

    /**
     * Core method for sending batch notifications and cleaning up invalid tokens
     */
    private boolean sendMulticastNotification(List<String> tokens, String title, String body,
                                            Map<String, String> data,
                                            AndroidConfig.Priority priority,
                                            String channelId) {
        if (firebaseMessaging.isEmpty()) {
            log.warn("Skipping multicast notification: Firebase Messaging is not initialized.");
            return false;
        }

        try {
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(priority)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setChannelId(channelId)
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").build())
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // Send to all devices in one batch
            BatchResponse response = firebaseMessaging.get().sendEachForMulticast(messageBuilder.build());
            log.info("Multicast sent to {} devices. Success: {}, Failure: {}",
                    tokens.size(), response.getSuccessCount(), response.getFailureCount());

            // 3. Handle Invalid Token Cleanup
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        FirebaseMessagingException ex = responses.get(i).getException();
                        log.warn("FCM send failed for token {}: {} - {}",
                                tokens.get(i), ex.getMessagingErrorCode(), ex.getMessage());
                        if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                                ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                            String failedToken = tokens.get(i);
                            log.warn("Removing invalid/unregistered token: {}", failedToken);
                            deviceTokenService.removeInvalidToken(failedToken);
                        }
                    }
                }
            }
            return response.getSuccessCount() > 0;
        } catch (FirebaseMessagingException e) {
            log.error("Critical failure sending multicast FCM notification", e);
            return false;
        }
    }

    /**
     * Helper to fetch tokens
     */
    private List<String> getUserTokens(UUID userId) {
        List<DeviceToken> deviceTokens = deviceTokenService.getUserDeviceTokens(userId);
        if (deviceTokens.isEmpty()) {
            log.debug("User {} has no registered devices", userId);
        }
        return deviceTokens.stream()
                .map(DeviceToken::getFcmToken)
                .collect(Collectors.toList());
    }

    /**
     * Send notification to a single device and clean up the token if it is invalid.
     */
    public void sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM token is null or empty, skipping notification");
            return;
        }

        if (firebaseMessaging.isEmpty()) {
            log.warn("Skipping single notification: Firebase Messaging is not initialized.");
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.NORMAL) // Replaced DEFAULT with NORMAL
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .setChannelId("default") // Generic channel
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").build())
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // Send the single message
            String response = firebaseMessaging.get().send(messageBuilder.build());
            log.debug("Successfully sent notification to device. Message ID: {}", response);

        } catch (FirebaseMessagingException e) {
            MessagingErrorCode errorCode = e.getMessagingErrorCode();

            // Handle token expiration, app uninstalls, or malformed tokens
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                log.warn("Device token is invalid/unregistered, removing from DB: {}", fcmToken);
                deviceTokenService.removeInvalidToken(fcmToken);
            } else {
                log.error("Failed to send single FCM notification. Error Code: {}", errorCode, e);
            }
        }
    }
}
