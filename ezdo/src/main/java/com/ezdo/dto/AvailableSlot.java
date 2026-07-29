package com.ezdo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AvailableSlot(
        LocalDateTime start,
        LocalDateTime end,
        UUID zoneId,
        String zoneName,
        CategoryResponse category
) {}