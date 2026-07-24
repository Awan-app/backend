package com.ezdo.dto.ai.decompose;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DecompositionUserContext(
    LocalDate today,
    String timezone,
    Integer preferredSessionDuration,
    Integer bufferBetweenSessions,
    List<CategoryOption> categories
) {
    public record CategoryOption(UUID id, String name) {}
}
