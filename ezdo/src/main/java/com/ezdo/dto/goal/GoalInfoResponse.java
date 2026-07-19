package com.ezdo.dto.goal;

import com.ezdo.entity.GoalStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoalInfoResponse(
    UUID id,
    String title,
    String description,
    GoalStatus status,
    LocalDate targetDate,
    Instant createdAt,
    boolean inbox,
    List<TaskInfoResponse> tasks
) {
}
