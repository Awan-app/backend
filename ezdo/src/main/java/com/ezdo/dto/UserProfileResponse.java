package com.ezdo.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    LocalDate birthDate,
    long points,
    int streak,
    int maxStreak,
    String profilePictureUrl,
    boolean isNew,
    PreferencesResponse preferences
) {
}
