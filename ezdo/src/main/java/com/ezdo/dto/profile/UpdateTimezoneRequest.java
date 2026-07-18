package com.ezdo.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record UpdateTimezoneRequest(
    @NotBlank(message = "Timezone is required")
    String timezone
) {}
