package com.ezdo.service;

import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Resolves "now" in the user's own timezone.
 *
 * <p>Every user-facing date in the app is local, never UTC — a fixed reference
 * timezone would roll a user's day over in the middle of their working hours.
 */
@Component
public class UserClockService {

    private static final ZoneId FALLBACK = ZoneId.of("UTC");

    public ZoneId zoneOf(User user) {
        Preferences prefs = user.getPreferences();
        if (prefs == null || prefs.getTimezone() == null) {
            return FALLBACK;
        }
        return ZoneId.of(prefs.getTimezone());
    }

    public LocalDate today(User user) {
        return LocalDate.now(zoneOf(user));
    }
}
