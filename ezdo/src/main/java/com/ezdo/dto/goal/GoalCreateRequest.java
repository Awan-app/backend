package com.ezdo.dto.goal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record GoalCreateRequest(
    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    @Future
    LocalDate targetDate,

    @Valid
    @Size(max = 50, message = "A goal can be created with at most 50 tasks at once")
    List<DraftTaskRequest> tasks
) {
    public GoalCreateRequest {
        if (tasks == null) tasks = List.of();
    }
}
