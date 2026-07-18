package com.ezdo.mapper;

import com.ezdo.dto.PreferencesResponse;
import com.ezdo.dto.UpdateProfileRequest;
import com.ezdo.dto.UserProfileResponse;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.exception.InvalidTimezoneException;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.ZoneId;

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

        Preferences preferences = user.getPreferences();
        if (preferences == null && hasPreferenceFields(request)) {
            preferences = new Preferences();
            preferences.setUser(user);
            user.setPreferences(preferences);
        }

        if (preferences != null) {
            if (request.timezone() != null) {
                try {
                    ZoneId.of(request.timezone());
                } catch (DateTimeException ex) {
                    throw new InvalidTimezoneException();
                }
                preferences.setTimezone(request.timezone());
            }
            if (request.preferredSessionDuration() != null) {
                preferences.setPreferredSessionDuration(request.preferredSessionDuration());
            }
            if (request.bufferBetweenSessions() != null) {
                preferences.setBufferBetweenSessions(request.bufferBetweenSessions());
            }
        }
    }

    private boolean hasPreferenceFields(UpdateProfileRequest request) {
        return request.timezone() != null || 
               request.preferredSessionDuration() != null || 
               request.bufferBetweenSessions() != null;
    }
}
