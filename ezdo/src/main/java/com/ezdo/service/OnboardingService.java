package com.ezdo.service;

import com.ezdo.dto.OnboardingRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.exception.OnboardingAlreadyCompletedException;
import com.ezdo.exception.InvalidTimezoneException;
import com.ezdo.mapper.UserMapper;
import com.ezdo.repository.PreferencesRepository;
import com.ezdo.repository.UserRepository;
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
    private final PreferencesRepository preferencesRepository;
    private final UserMapper userMapper;

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

        Preferences preferences = new Preferences();
        preferences.setUser(user);
        user.setPreferences(preferences);


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

        userRepository.save(user);

        return userMapper.toProfileResponse(user);
    }
}
