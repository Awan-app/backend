package com.ezdo.dto.ai;

import java.util.List;

public record GoalProposal(
    String title,
    String description,
    String targetDate,
    List<TaskProposal> tasks
) {
}
