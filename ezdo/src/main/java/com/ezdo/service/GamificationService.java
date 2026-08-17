package com.ezdo.service;

import com.ezdo.dto.gamification.PointsDelta;
import com.ezdo.dto.gamification.CompletionReward;
import com.ezdo.dto.gamification.StreakDelta;
import com.ezdo.dto.profile.UserProgressResponse;
import com.ezdo.entity.Session;
import com.ezdo.entity.Streak;
import com.ezdo.entity.User;
import com.ezdo.entity.Wallet;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.StreakRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.util.DateRangeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserRepository userRepository;
    private final StreakRepository streakRepository;
    private final WalletService walletService;
    private final StreakService streakService;
    private final UserClockService userClockService;

    @Transactional
    public CompletionReward onSessionCompleted(User user, Session session) {
        Wallet wallet = user.getWallet();
        Streak streak = user.getStreak();

        Integer points = session.getTask().getEstimatedPoints();
        boolean awarded = points != null && points > 0;
        long oldPoints = wallet.getPoints();
        if (awarded) {
            walletService.credit(user, points);
        }
        long newPoints = wallet.getPoints();

        LocalDate today = userClockService.today(user);
        int oldStreak = streak.effectiveStreak(today);
        int oldMaxStreak = streak.getMaxStreak();
        streakService.recordQualifyingActivity(user);
        int newStreak = streak.effectiveStreak(today);
        int newMaxStreak = streak.getMaxStreak();

        return new CompletionReward(
            new PointsDelta(awarded, awarded ? points : 0, oldPoints, newPoints),
            new StreakDelta(
                oldStreak != newStreak,
                oldStreak,
                newStreak,
                newMaxStreak > oldMaxStreak,
                oldMaxStreak,
                newMaxStreak
            )
        );
    }

    @Transactional(readOnly = true)
    public CompletionReward currentResult(User user) {
        Wallet wallet = user.getWallet();
        Streak streak = user.getStreak();
        long points = wallet.getPoints();
        int currentStreak = streak.effectiveStreak(userClockService.today(user));
        int maxStreak = streak.getMaxStreak();
        return new CompletionReward(
            new PointsDelta(false, 0, points, points),
            new StreakDelta(false, currentStreak, currentStreak, false, maxStreak, maxStreak)
        );
    }

    @Transactional(readOnly = true)
    public UserProgressResponse getProgress(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserProgressResponse.of(user.getWallet(), user.getStreak(), userClockService.today(user));
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getActivityDates(UUID userId, LocalDate startDate, LocalDate endDate) {
        DateRangeValidator.validate(startDate, endDate);
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return streakRepository.findActivityDates(userId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public LocalDate getLastActivityDate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return user.getStreak().getLastActivityDate();
    }
}
