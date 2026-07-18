package com.ezdo.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record UpdateNameRequest(
    @NotBlank(message = "First name is required")
    String firstName,

    @NotBlank(message = "Last name is required")
    String lastName
) {}
