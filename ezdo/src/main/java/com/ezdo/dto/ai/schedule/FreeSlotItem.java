package com.ezdo.dto.ai.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalTime;
import java.util.UUID;

/**
 * A schedulable free gap within a user's day, already computed by
 * {@link com.ezdo.service.AvailabilityService} (existing sessions already
 * subtracted from zone time). Sent to the AI as part of {@link DaySlot}.
 *
 * <p>{@code zoneId} is annotated {@link JsonIgnore} so it is never included
 * in the JSON payload sent to the AI — exposing a raw UUID to the model only
 * adds a way for it to be wrong. Zone assignment is resolved server-side by
 * matching the AI's chosen time against this record's {@code start}/{@code end}.
 */
public record FreeSlotItem(
    LocalTime start,
    LocalTime end,
    long durationMinutes,       // pre-computed for the AI's convenience
    @JsonIgnore UUID zoneId,    // server-side only — NOT sent to AI
    String zoneName,            // null when outside any zone (unzoned gap)
    String categoryName         // null when outside any zone or zone has no category
) {}
