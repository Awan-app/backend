package com.ezdo.dto;

public record RefreshResponse(
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken
) {}
