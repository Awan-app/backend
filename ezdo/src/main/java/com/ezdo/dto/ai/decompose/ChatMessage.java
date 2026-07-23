package com.ezdo.dto.ai.decompose;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ChatMessage(
    UUID sessionId, // null for the first message in the session.
    @NotBlank
    String message
) {
}
