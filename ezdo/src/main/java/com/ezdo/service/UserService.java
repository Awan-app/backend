package com.ezdo.service;

import com.ezdo.dto.UpdateProfileRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.dto.profile.*;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.exception.*;
import com.ezdo.mapper.UserMapper;
import com.ezdo.repository.UserRepository;
import com.ezdo.scheduler.DailySummarySchedulerService;
import com.ezdo.scheduler.DailySpinSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final StoreService storeService;
    private final CloudinaryService cloudinaryService;
    private final DailySummarySchedulerService dailySummarySchedulerService;
    private final DailySpinSchedulerService dailySpinSchedulerService;
    private final WalletService walletService;

    private static final long MAX_PROFILE_PICTURE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return profile(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> isNew(UUID userId) {
        return Map.of("isNew", findUser(userId).getIsNew());
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.timezone() != null) {
            try {
                ZoneId.of(request.timezone());
            } catch (DateTimeException ex) {
                throw new InvalidTimezoneException();
            }
        }
        if (request.wakeupTime() != null && request.sleepTime() != null
                && request.wakeupTime().equals(request.sleepTime())) {
            throw new InvalidSleepScheduleException(
                request.wakeupTime(), request.sleepTime());
        }

        userMapper.updateProfileFromRequest(request, user);

        User saved = userRepository.save(user);
        dailySummarySchedulerService.syncDailySummary(saved);
        dailySpinSchedulerService.syncDailySpin(saved);

        return profile(saved);
    }

    public ProfilePictureResponse updateProfilePicture(UUID userId, MultipartFile file) {
        validateProfilePicture(file);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        String url = cloudinaryService.uploadProfilePicture(userId, file);
        userRepository.updateProfilePictureUrl(userId, url);

        return new ProfilePictureResponse(url);
    }

    /** Idempotent: removing a picture the user does not have is a no-op, not an error. */
    public void removeProfilePicture(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        cloudinaryService.deleteProfilePicture(userId);
        userRepository.updateProfilePictureUrl(userId, null);
    }

    private void validateProfilePicture(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ProfilePictureEmptyException();
        }
        if (file.getSize() > MAX_PROFILE_PICTURE_SIZE) {
            throw new ProfilePictureTooLargeException(file.getSize(), MAX_PROFILE_PICTURE_SIZE);
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new UnsupportedImageTypeException(file.getContentType(), String.join(", ", ALLOWED_IMAGE_TYPES));
        }
    }

    @Transactional
    public UserProfileResponse updateName(UUID userId, UpdateNameRequest request) {
        User user = findUser(userId);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        userRepository.save(user);

        return profile(user);
    }

    @Transactional
    public UserProfileResponse updateBirthDate(UUID userId, UpdateBirthDateRequest request) {
        User user = findUser(userId);

        user.setBirthDate(request.birthDate());

        userRepository.save(user);

        return profile(user);
    }

    @Transactional
    public UserProfileResponse updateTimezone(UUID userId, UpdateTimezoneRequest request) {
        Preferences preferences = findPreferences(userId);

        preferences.setTimezone(request.timezone());

        userRepository.save(preferences.getUser());
        dailySpinSchedulerService.syncDailySpin(preferences.getUser());

        return profile(preferences.getUser());
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

        User saved = userRepository.save(preferences.getUser());
        dailySummarySchedulerService.syncDailySummary(saved);

        return profile(saved);
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

        User saved = userRepository.save(preferences.getUser());
        dailySummarySchedulerService.syncDailySummary(saved);

        return profile(saved);
    }

    @Transactional
    public UserProfileResponse updateSchedulingType(
        UUID userId,
        UpdateSchedulingTypeRequest request
    ) {
        Preferences preferences = findPreferences(userId);

        preferences.setSchedulingType(request.schedulingType());

        userRepository.save(preferences.getUser());

        return profile(preferences.getUser());
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    }

    private UserProfileResponse profile(User user) {
        return userMapper.toProfileResponse(user, storeService.getEquipped(user.getId()));
    }

    private Preferences findPreferences(UUID userId) {
        return findUser(userId).getPreferences();
    }

    @Transactional
    public long setPoints(UUID userId, long points) {
        return walletService.setPoints(findUser(userId), points).getPoints();
    }
}
