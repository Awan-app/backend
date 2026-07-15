package com.ezdo.dto;

public record VerifyOtpResponse(
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    UserDto user
) {
}
