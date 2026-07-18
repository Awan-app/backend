package com.ezdo.dto.goal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ConfirmGoalRequest(

    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    LocalDate targetDate,

    @NotEmpty
    @Valid
    List<DraftTaskRequest> tasks
) {}
