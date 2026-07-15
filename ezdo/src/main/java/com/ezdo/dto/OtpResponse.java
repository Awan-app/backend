package com.ezdo.dto;

public record OtpResponse(
    int expiresInSeconds,
    int resendAvailableInSeconds
) {}
