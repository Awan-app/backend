package com.ezdo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RefreshRequest(
        @NotBlank
        String refreshToken,

        @NotNull
        UUID deviceId
) {}
