package com.ezdo.dto;

public record VerifyOtpResponse(
        String status,
        boolean isNewUser,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        UserDto user
) {}
