package com.ezdo.dto;

import java.time.Instant;
import java.util.UUID;

public record DeviceTokenResponse(
    UUID id,
    String deviceId,
    String deviceType,
    Instant createdAt,
    Instant updatedAt
) {
}
