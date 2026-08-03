package com.ezdo.mapper;

import com.ezdo.dto.PreferencesResponse;
import com.ezdo.dto.UpdateProfileRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user) {
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
                p.getSchedulingType()
            );
        }

        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getBirthDate(),
            user.getPoints(),
            user.getStreak(),
            user.getMaxStreak(),
            user.getProfilePictureUrl(),
            user.getIsNew(),
            preferencesResponse
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
    }
}
