package com.ezdo.service;

import com.ezdo.entity.DeviceToken;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.DeviceTokenRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * Register or update a device token for a user
     * If the device already exists, update its FCM token
     */
    @Transactional
    public DeviceToken registerDeviceToken(UUID userId, String deviceId, String fcmToken, String deviceType) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Optional<DeviceToken> existingToken = deviceTokenRepository.findByUserIdAndDeviceId(userId, deviceId);

        if (existingToken.isPresent()) {
            // Update existing device token
            DeviceToken deviceToken = existingToken.get();
            deviceToken.setFcmToken(fcmToken);
            if (deviceType != null) {
                deviceToken.setDeviceType(deviceType);
            }
            log.info("Updated FCM token for user {} device {}", userId, deviceId);
            return deviceTokenRepository.save(deviceToken);
        } else {
            // Create new device token
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .fcmToken(fcmToken)
                    .deviceType(deviceType)
                    .build();
            log.info("Registered new device token for user {} device {}", userId, deviceId);
            return deviceTokenRepository.save(deviceToken);
        }
    }

    /**
     * Remove a specific device token
     */
    @Transactional
    public void removeDeviceToken(UUID userId, String deviceId) {
        deviceTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
        log.info("Removed device token for user {} device {}", userId, deviceId);
    }

    /**
     * Remove all device tokens for a user
     */
    @Transactional
    public void removeAllDeviceTokens(UUID userId) {
        deviceTokenRepository.deleteByUserId(userId);
        log.info("Removed all device tokens for user {}", userId);
    }

    /**
     * Get all device tokens for a user
     */
    @Transactional(readOnly = true)
    public List<DeviceToken> getUserDeviceTokens(UUID userId) {
        return deviceTokenRepository.findByUserId(userId);
    }

    /**
     * Remove invalid/unregistered FCM token
     */
    @Transactional
    public void removeInvalidToken(String fcmToken) {
        Optional<DeviceToken> deviceToken = deviceTokenRepository.findByFcmToken(fcmToken);
        deviceToken.ifPresent(token -> {
            deviceTokenRepository.delete(token);
            log.info("Removed invalid FCM token for user {} device {}",
                    token.getUser().getId(), token.getDeviceId());
        });
    }
}
