package com.ezdo.service;

import com.ezdo.dto.profile.UserProgressResponse;
import com.ezdo.entity.Session;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final StreakService streakService;
    private final UserClockService userClockService;

    @Transactional
    public void onSessionCompleted(User user, Session session) {
        Integer award = session.getTask().getEstimatedPoints();
        if (award != null && award > 0) {
            walletService.credit(user, award);
        }
        streakService.recordQualifyingActivity(user);
    }

    @Transactional(readOnly = true)
    public UserProgressResponse getProgress(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserProgressResponse.of(user.getWallet(), user.getStreak(), userClockService.today(user));
    }
}
