package com.ezdo.dto.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DraftTaskRequest(
    @NotBlank
    String tempId,                 // client-generated, only used to wire dependsOnTempIds

    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    Integer estimatedDuration,

    Boolean mandatory,

    Integer estimatedPoints,

    Boolean allowTaskSplitting,

    List<String> dependsOnTempIds
) {
}
