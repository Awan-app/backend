package com.ezdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** {@code idToken} is the Firebase ID token returned by the client-side Firebase SDK. */
public record FirebaseLoginRequest(
    @NotBlank String idToken,
    @NotNull UUID deviceId
) {}
