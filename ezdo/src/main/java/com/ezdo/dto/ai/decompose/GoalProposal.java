package com.ezdo.dto.ai.decompose;

import java.util.List;

public record GoalProposal(
    String title,
    String description,
    String targetDate,
    List<TaskProposal> tasks
) {
}
