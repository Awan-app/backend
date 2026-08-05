package com.ezdo.dto;

/**
 * Returned by every login path (email OTP, Google via Firebase) so a client
 * cannot tell them apart. Field order and names match the former
 * {@code VerifyOtpResponse} exactly, so the wire shape is unchanged.
 */
public record AuthResponse(
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    UserDto user
) {}
