package com.ezdo.dto.ai.image;

import java.time.LocalDateTime;

/**
 * One concrete time slot the planning model read out of the image, already resolved
 * to the user's local wall clock. Both ends are required; drafts missing either are
 * dropped before they reach the client.
 */
public record ExtractedSession(
    LocalDateTime start,
    LocalDateTime end
) {
}
