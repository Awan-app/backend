package com.ezdo.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
    @NotBlank(message = "Device ID is required")
    String deviceId,

    @NotBlank(message = "FCM token is required")
    String fcmToken,

    String deviceType  // Optional: "ANDROID", "IOS", "WEB"
) {
}
