package com.ezdo.service.ai;

import com.ezdo.dto.ai.AiUserPreferencesContext;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.CategoryRepository;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Builds a fresh {@link AiUserPreferencesContext} for a single AI call: the user's
 * real categories, scheduling preferences, and the current date-time resolved in their
 * timezone. Shared by every service that grounds a model call in per-user data.
 * Never cached across calls, so edits the user makes to categories/preferences
 * mid-session are always reflected on the next call.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public AiUserPreferencesContext buildFor(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        Preferences prefs = user.getPreferences();

        ZoneId zone = ZoneId.of("UTC");
        if (prefs != null && prefs.getTimezone() != null) {
            try {
                zone = ZoneId.of(prefs.getTimezone());
            } catch (Exception e) {
                log.warn("Invalid timezone '{}' for user {}, defaulting to UTC",
                    prefs.getTimezone(), userId);
            }
        }

        List<AiUserPreferencesContext.CategoryOption> categories = categoryRepository.findAll()
            .stream()
            .map(c -> new AiUserPreferencesContext.CategoryOption(c.getId(), c.getName()))
            .toList();

        return new AiUserPreferencesContext(
            LocalDateTime.now(zone),
            prefs != null ? prefs.getTimezone() : null,
            prefs != null ? prefs.getPreferredSessionDuration() : null,
            prefs != null ? prefs.getBufferBetweenSessions() : null,
            prefs != null ? prefs.getWakeupTime() : null,
            prefs != null ? prefs.getSleepTime() : null,
            categories
        );
    }
}
