package com.ezdo.service.ai;

import com.ezdo.dto.ai.plan.ScheduleContext;

import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

/**
 * Renders a {@link ScheduleContext} into the "AVAILABILITY" prompt section, appended
 * to the system message after {@link UserContextRenderer}'s block.
 *
 * <p>Booked items are rendered with their cost signals (locked, mandatory, points)
 * because the model is asked to choose the least damaging collision when the calendar
 * leaves it no free time.
 */
public final class ScheduleContextRenderer {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private ScheduleContextRenderer() {
    }

    public static String render(ScheduleContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=====================================================================\n");
        sb.append("AVAILABILITY (the user's local timezone)\n");
        sb.append("=====================================================================\n");
        sb.append("\"free\" is time you may schedule into. \"busy\" is an existing session — ")
            .append("avoid it, and only overlap one as a last resort, cheapest first.\n");

        if (context == null || context.days() == null || context.days().isEmpty()) {
            sb.append("(no availability information is known for this user)\n");
            return sb.toString();
        }

        for (ScheduleContext.ScheduleDay day : context.days()) {
            sb.append(displayName(day)).append(" ").append(day.date()).append("\n");

            boolean hasFree = day.free() != null && !day.free().isEmpty();
            boolean hasBooked = day.booked() != null && !day.booked().isEmpty();

            if (!hasFree && !hasBooked) {
                sb.append("  fully free — no zones and nothing booked\n");
                continue;
            }
            if (!hasFree) {
                sb.append("  free — none\n");
            } else {
                for (ScheduleContext.FreeSlot slot : day.free()) {
                    sb.append("  free ").append(slot.start().format(TIME))
                        .append("-").append(slot.end().format(TIME))
                        .append(renderZone(slot)).append("\n");
                }
            }
            if (hasBooked) {
                for (ScheduleContext.BookedItem item : day.booked()) {
                    sb.append("  busy ").append(item.start().format(TIME))
                        .append("-").append(item.end().format(TIME))
                        .append(" \"").append(item.taskTitle()).append("\"")
                        .append(renderCost(item)).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static String renderZone(ScheduleContext.FreeSlot slot) {
        if (slot.zoneName() == null) {
            return " (no zone)";
        }
        if (slot.categoryName() == null) {
            return " (zone \"" + slot.zoneName() + "\", no category)";
        }
        return " (zone \"" + slot.zoneName() + "\", category " + slot.categoryName() + ")";
    }

    /** The signals the model ranks collisions by — see the conflict rules in the system prompt. */
    private static String renderCost(ScheduleContext.BookedItem item) {
        StringJoiner parts = new StringJoiner(", ", " (", ")");
        parts.add(item.locked() ? "locked" : "unlocked");
        parts.add(item.mandatory() ? "mandatory" : "optional");
        if (item.points() != null) {
            parts.add(item.points() + "pts");
        }
        if (item.status() != null) {
            parts.add(item.status().name());
        }
        return parts.toString();
    }

    /** "Thu" rather than "THURSDAY" — the model reads prose, not enum constants. */
    private static String displayName(ScheduleContext.ScheduleDay day) {
        String name = day.dayOfWeek().name();
        return name.charAt(0) + name.substring(1, 3).toLowerCase();
    }
}
