package com.ezdo.dto;

import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String name
) {}
