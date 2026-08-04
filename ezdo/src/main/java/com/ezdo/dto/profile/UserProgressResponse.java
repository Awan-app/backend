package com.ezdo.dto.profile;

import com.ezdo.entity.Streak;
import com.ezdo.entity.Wallet;

import java.time.LocalDate;

public record UserProgressResponse(
    long points,
    int streak,
    int maxStreak
) {

    public static UserProgressResponse of(Wallet wallet, Streak streak, LocalDate today) {
        return new UserProgressResponse(
            wallet != null ? wallet.getPoints() : 0L,
            streak != null ? streak.effectiveStreak(today) : 0,
            streak != null ? streak.getMaxStreak() : 0
        );
    }
}
