package com.ezdo.dto.ai.decompose;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatMessage(
    UUID sessionId, // null for the first message in the session.
    @NotBlank

    @Size(max = 4000, message = "Message must be at most 4000 characters")
    String message
) {
}
