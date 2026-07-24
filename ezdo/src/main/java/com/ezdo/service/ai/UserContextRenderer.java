package com.ezdo.service.ai;

import com.ezdo.dto.ai.decompose.DecompositionUserContext;

/**
 * Renders a {@link DecompositionUserContext} into the "USER CONTEXT" prompt section
 * shared by every AI-facing prompt builder that needs category/preference grounding
 * (goal decomposition, standalone task enrichment).
 */
public final class UserContextRenderer {

    private UserContextRenderer() {
    }

    public static String render(DecompositionUserContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n=====================================================================\n");
        sb.append("USER CONTEXT\n");
        sb.append("=====================================================================\n");
        sb.append("Today's date: ").append(ctx.today()).append("\n");

        if (ctx.timezone() != null) {
            sb.append("User's timezone: ").append(ctx.timezone()).append("\n");
        }
        if (ctx.preferredSessionDuration() != null) {
            sb.append("User's preferred focused-session length: ")
                .append(ctx.preferredSessionDuration()).append(" minutes. ")
                .append("Prefer sizing task durations around this rather than a generic range ")
                .append("where the two conflict.\n");
        }
        if (ctx.bufferBetweenSessions() != null) {
            sb.append("User's preferred buffer between sessions: ")
                .append(ctx.bufferBetweenSessions()).append(" minutes.\n");
        }

        sb.append("Categories — use the EXACT id and name below for \"category\", ")
            .append("or null if none fit. Never invent an id or name not listed here:\n");
        if (ctx.categories() == null || ctx.categories().isEmpty()) {
            sb.append("- (this user has no categories yet; category must be null)\n");
        } else {
            for (DecompositionUserContext.CategoryOption c : ctx.categories()) {
                sb.append("- id: ").append(c.id()).append(", name: \"").append(c.name()).append("\"\n");
            }
        }
        return sb.toString();
    }
}