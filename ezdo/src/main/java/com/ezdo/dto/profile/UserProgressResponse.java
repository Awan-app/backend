package com.ezdo.dto.profile;

public record UserProgressResponse(
    Integer points,
    Integer streak,
    Integer maxStreak
) {}
