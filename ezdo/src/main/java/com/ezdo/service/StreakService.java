package com.ezdo.service;

import com.ezdo.entity.Streak;
import com.ezdo.entity.User;
import com.ezdo.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakRepository streakRepository;
    private final UserClockService userClockService;

    @Transactional
    public Streak getOrCreate(User user) {
        Streak streak = user.getStreak();
        if (streak != null) {
            return streak;
        }

        streak = Streak.builder().user(user).build();
        user.setStreak(streak);
        return streakRepository.save(streak);
    }

    @Transactional
    public Streak recordQualifyingActivity(User user) {
        LocalDate today = userClockService.today(user);
        Streak streak = getOrCreate(user);
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
