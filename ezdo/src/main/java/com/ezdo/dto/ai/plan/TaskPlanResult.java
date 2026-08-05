package com.ezdo.dto.ai.plan;

import java.util.List;

/** The {@code {"tasks":[...]}} envelope the planning stage is contracted to return. */
public record TaskPlanResult(
    List<DraftedTask> tasks
) {
}
