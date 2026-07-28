package com.ezdo.dto.ai.image;

import java.util.List;

/** The {@code {"tasks":[...]}} envelope the planning stage is contracted to return. */
public record ImageExtractionResult(
    List<ExtractedTask> tasks
) {
}
