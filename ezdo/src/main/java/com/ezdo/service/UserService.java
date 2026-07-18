package com.ezdo.service;

import com.ezdo.dto.UpdateProfileRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.dto.profile.*;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.exception.InsufficientPointsException;
import com.ezdo.exception.InvalidSleepScheduleException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.mapper.UserMapper;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getPreferences() == null) {
            Preferences preferences = new Preferences();
            preferences.setUser(user);
            user.setPreferences(preferences);
        }

        userMapper.updateProfileFromRequest(request, user);

        userRepository.save(user);

        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateName(UUID userId, UpdateNameRequest request) {
        User user = findUser(userId);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        userRepository.save(user);

        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateBirthDate(UUID userId, UpdateBirthDateRequest request) {
        User user = findUser(userId);

        user.setBirthDate(request.birthDate());

        userRepository.save(user);

        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProgressResponse incrementStreak(UUID userId) {
        User user = findUser(userId);

        user.setStreak(user.getStreak() + 1);

        if (user.getStreak() > user.getMaxStreak()) {
            user.setMaxStreak(user.getStreak());
        }

        userRepository.save(user);

        return new UserProgressResponse(
            user.getPoints(), user.getStreak(), user.getMaxStreak());
    }

    @Transactional
    public UserProgressResponse resetStreak(UUID userId) {
        User user = findUser(userId);

        user.setStreak(0);

        userRepository.save(user);

        return new UserProgressResponse(
            user.getPoints(), user.getStreak(), user.getMaxStreak());
    }

    @Transactional
    public UserProgressResponse awardPoints(UUID userId, AwardPointsRequest request) {
        User user = findUser(userId);

        // TODO: Add points validation rules...
        user.setPoints(user.getPoints() + request.points());

        userRepository.save(user);

        return new UserProgressResponse(
            user.getPoints(), user.getStreak(), user.getMaxStreak());
    }

    @Transactional
    public UserProgressResponse deductPoints(UUID userId, DeductPointsRequest request) {
        User user = findUser(userId);

        // TODO: Add points validation rules...
        if (request.points() > user.getPoints()) {
            throw new InsufficientPointsException(user.getPoints(), request.points());
        }

        user.setPoints(user.getPoints() - request.points());

        userRepository.save(user);

        return new UserProgressResponse(
            user.getPoints(), user.getStreak(), user.getMaxStreak());
    }

    @Transactional
    public UserProfileResponse updateTimezone(UUID userId, UpdateTimezoneRequest request) {
        Preferences preferences = findPreferences(userId);

        preferences.setTimezone(request.timezone());

        userRepository.save(preferences.getUser());

        return userMapper.toProfileResponse(preferences.getUser());
    }

    @Transactional
    public UserProfileResponse updateSessionSettings(
        UUID userId,
        UpdateSessionSettingsRequest request
    ) {
        Preferences preferences = findPreferences(userId);

        preferences.setPreferredSessionDuration(
            request.preferredSessionDuration());
        preferences.setBufferBetweenSessions(
            request.bufferBetweenSessions());

        userRepository.save(preferences.getUser());

        return userMapper.toProfileResponse(preferences.getUser());
    }

    @Transactional
    public UserProfileResponse updateSleepSchedule(
        UUID userId,
        UpdateSleepScheduleRequest request
    ) {
        if (request.wakeupTime().equals(request.sleepTime())) {
            throw new InvalidSleepScheduleException(
                request.wakeupTime(), request.sleepTime());
        }

        Preferences preferences = findPreferences(userId);

        preferences.setWakeupTime(request.wakeupTime());
        preferences.setSleepTime(request.sleepTime());

        userRepository.save(preferences.getUser());

        return userMapper.toProfileResponse(preferences.getUser());
    }

    @Transactional
    public UserProfileResponse updateSchedulingType(
        UUID userId,
        UpdateSchedulingTypeRequest request
    ) {
        Preferences preferences = findPreferences(userId);

        preferences.setSchedulingType(request.schedulingType());

        userRepository.save(preferences.getUser());

        return userMapper.toProfileResponse(preferences.getUser());
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    }

    private Preferences findPreferences(UUID userId) {
        return findUser(userId).getPreferences();
    }
}
