package com.ezdo.controller;

import com.ezdo.dto.DeviceTokenResponse;
import com.ezdo.dto.FcmTokenRequest;
import com.ezdo.entity.DeviceToken;
import com.ezdo.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    /**
     * Register or update a device token
     */
    @PostMapping
    public ResponseEntity<DeviceTokenResponse> registerDeviceToken(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        DeviceToken deviceToken = deviceTokenService.registerDeviceToken(
                userId,
                request.deviceId(),
                request.fcmToken(),
                request.deviceType()
        );
        return ResponseEntity.ok(toResponse(deviceToken));
    }

    /**
     * Get all registered devices for the current user
     */
    @GetMapping
    public ResponseEntity<List<DeviceTokenResponse>> getUserDevices(
            @AuthenticationPrincipal UUID userId
    ) {
        List<DeviceToken> deviceTokens = deviceTokenService.getUserDeviceTokens(userId);
        List<DeviceTokenResponse> responses = deviceTokens.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }


    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> removeDeviceToken(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String deviceId
    ) {
        deviceTokenService.removeDeviceToken(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove all device tokens for the current user
     */
    @DeleteMapping
    public ResponseEntity<Void> removeAllDeviceTokens(
            @AuthenticationPrincipal UUID userId
    ) {
        deviceTokenService.removeAllDeviceTokens(userId);
        return ResponseEntity.noContent().build();
    }

    private DeviceTokenResponse toResponse(DeviceToken deviceToken) {
        return new DeviceTokenResponse(
                deviceToken.getId(),
                deviceToken.getDeviceId(),
                deviceToken.getDeviceType(),
                deviceToken.getCreatedAt(),
                deviceToken.getUpdatedAt()
        );
    }
}
