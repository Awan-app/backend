package com.ezdo.service;

import com.ezdo.dto.OnboardingRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.exception.OnboardingAlreadyCompletedException;
import com.ezdo.exception.InvalidTimezoneException;
import com.ezdo.mapper.UserMapper;
import com.ezdo.repository.UserRepository;
import com.ezdo.scheduler.DailySummarySchedulerService;
import com.ezdo.scheduler.DailySpinSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final StoreService storeService;
    private final DailySummarySchedulerService dailySummarySchedulerService;
    private final DailySpinSchedulerService dailySpinSchedulerService;

    @Transactional
    public UserProfileResponse completeOnboarding(UUID userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        if (!Boolean.TRUE.equals(user.getIsNew())) {
            throw new OnboardingAlreadyCompletedException();
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBirthDate(request.birthDate());
        user.setIsNew(false);

        Preferences preferences = user.getPreferences();

        try {
            ZoneId.of(request.timezone());
        } catch (DateTimeException ex) {
            throw new InvalidTimezoneException();
        }

        preferences.setTimezone(request.timezone());
        preferences.setPreferredSessionDuration(request.preferredSessionDuration());
        preferences.setBufferBetweenSessions(request.bufferBetweenSessions());
        preferences.setWakeupTime(request.wakeupTime());
        preferences.setSleepTime(request.sleepTime());
        preferences.setSchedulingType(request.schedulingType());
        if (request.notificationsEnabled() != null) {
            preferences.setNotificationsEnabled(request.notificationsEnabled());
        }

        User saved = userRepository.save(user);
        dailySummarySchedulerService.syncDailySummary(saved);
        dailySpinSchedulerService.syncDailySpin(saved);

        return userMapper.toProfileResponse(saved, storeService.getEquipped(saved.getId()));
    }
}
