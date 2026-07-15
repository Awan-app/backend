package com.ezdo.dto;

public record OtpResponse(
        String status,
        int expiresInSeconds,
        int resendAvailableInSeconds
) {}
