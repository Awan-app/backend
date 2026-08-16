package com.ezdo.mapper;

import com.ezdo.dto.PreferencesResponse;
import com.ezdo.dto.UpdateProfileRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.dto.profile.UserProgressResponse;
import com.ezdo.dto.store.EquippedItemResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.service.UserClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final UserClockService userClockService;

    public UserProfileResponse toProfileResponse(User user, List<EquippedItemResponse> equippedItems) {
        if (user == null) {
            return null;
        }

        Preferences p = user.getPreferences();
        PreferencesResponse preferencesResponse = null;
        if (p != null) {
            preferencesResponse = new PreferencesResponse(
                p.getTimezone(),
                p.getPreferredSessionDuration(),
                p.getBufferBetweenSessions(),
                p.getWakeupTime(),
                p.getSleepTime(),
                p.getSchedulingType(),
                p.getDailySummaryEnabled(),
                p.getNotificationsEnabled()
            );
        }

        UserProgressResponse progress = UserProgressResponse.of(
            user.getWallet(), user.getStreak(), userClockService.today(user));

        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getBirthDate(),
            progress.points(),
            progress.streak(),
            progress.maxStreak(),
            user.getProfilePictureUrl(),
            user.getIsNew(),
            preferencesResponse,
            equippedItems
        );
    }

    public void updateProfileFromRequest(UpdateProfileRequest request, User user) {
        if (request == null || user == null) {
            return;
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }

        Preferences p = user.getPreferences();
        if (p == null) {
            return;
        }

        if (request.timezone() != null) {
            p.setTimezone(request.timezone());
        }
        if (request.preferredSessionDuration() != null) {
            p.setPreferredSessionDuration(request.preferredSessionDuration());
        }
        if (request.bufferBetweenSessions() != null) {
            p.setBufferBetweenSessions(request.bufferBetweenSessions());
        }
        if (request.wakeupTime() != null) {
            p.setWakeupTime(request.wakeupTime());
        }
        if (request.sleepTime() != null) {
            p.setSleepTime(request.sleepTime());
        }
        if (request.schedulingType() != null) {
            p.setSchedulingType(request.schedulingType());
        }
        if (request.dailySummaryEnabled() != null) {
            p.setDailySummaryEnabled(request.dailySummaryEnabled());
        }
        if (request.notificationsEnabled() != null) {
            p.setNotificationsEnabled(request.notificationsEnabled());
        }
    }
}
