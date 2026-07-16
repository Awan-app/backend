package com.ezdo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyOtpRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Pattern(regexp = "\\d{6}")
        String code,

        @NotNull
        UUID deviceId
) {
}
