package com.ezdo.service.ai;

import com.ezdo.dto.ai.AiUserPreferencesContext;

import java.time.format.DateTimeFormatter;

/**
 * Renders a {@link AiUserPreferencesContext} into the "USER CONTEXT" prompt section
 * shared by every AI-facing prompt builder that needs category/preference grounding
 * (goal decomposition, standalone task enrichment, image-to-tasks extraction).
 * Appended to the static system prompt rather than sent as a separate message.
 */
public final class UserContextRenderer {

    private static final DateTimeFormatter DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME =
        DateTimeFormatter.ofPattern("HH:mm");

    private UserContextRenderer() {
    }

    public static String render(AiUserPreferencesContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n=====================================================================\n");
        sb.append("USER CONTEXT\n");
        sb.append("=====================================================================\n");
        sb.append("Current date and time in the user's local timezone: ")
            .append(ctx.now().format(DATE_TIME))
            .append(" (")
            .append(displayName(ctx))
            .append(")\n");
        sb.append("Today's date: ").append(ctx.today()).append("\n");

        if (ctx.timezone() != null) {
            sb.append("User's timezone: ").append(ctx.timezone()).append("\n");
        }
        sb.append("Every date and time you produce is in this local timezone. ")
            .append("Never schedule anything earlier than the current date and time above.\n");

        if (ctx.wakeupTime() != null && ctx.sleepTime() != null) {
            sb.append("User's typical waking hours: ")
                .append(ctx.wakeupTime().format(TIME))
                .append(" to ")
                .append(ctx.sleepTime().format(TIME))
                .append(" — do not place sessions outside them unless the user's own wording ")
                .append("explicitly asks for a time in that window.\n");
        }
        if (ctx.preferredSessionDuration() != null) {
            sb.append("User's preferred focused-session length: ")
                .append(ctx.preferredSessionDuration()).append(" minutes. ")
                .append("Prefer sizing individual task durations around this rather than the generic ")
                .append("15-minutes-to-half-a-day range where the two conflict.\n");
        }
        if (ctx.bufferBetweenSessions() != null) {
            sb.append("User's preferred buffer between sessions: ")
                .append(ctx.bufferBetweenSessions()).append(" minutes.\n");
        }

        sb.append("Categories — use the EXACT id and name below for a task's \"category\", ")
            .append("or null if none fit. Never invent an id or name not listed here:\n");
        if (ctx.categories() == null || ctx.categories().isEmpty()) {
            sb.append("- (this user has no categories yet; every task's category must be null)\n");
        } else {
            for (AiUserPreferencesContext.CategoryOption c : ctx.categories()) {
                sb.append("- id: ").append(c.id()).append(", name: \"").append(c.name()).append("\"\n");
            }
        }
        return sb.toString();
    }

    /** "Tuesday" rather than "TUESDAY" — the model reads prose, not enum constants. */
    private static String displayName(AiUserPreferencesContext ctx) {
        String name = ctx.dayOfWeek().name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
