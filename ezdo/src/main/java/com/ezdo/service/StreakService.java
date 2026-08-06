package com.ezdo.service;

import com.ezdo.entity.Streak;
import com.ezdo.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserClockService userClockService;

    @Transactional
    public Streak recordQualifyingActivity(User user) {
        LocalDate today = userClockService.today(user);
        Streak streak = user.getStreak();
        LocalDate last = streak.getLastActivityDate();

        if (today.equals(last)) {
            return streak;
        }

        if (last != null && last.equals(today.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            // First activity ever, or a day was missed — start over.
            streak.setCurrentStreak(1);
        }

        if (streak.getCurrentStreak() > streak.getMaxStreak()) {
            streak.setMaxStreak(streak.getCurrentStreak());
        }
        streak.setLastActivityDate(today);

        return streak;
    }
}
