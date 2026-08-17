package com.ezdo.service.ai.rag;

import com.ezdo.dto.ai.RelatedGoalContext;

import java.util.List;

public final class RelatedWorkRenderer {

    private RelatedWorkRenderer() {
    }

    public static String render(List<RelatedGoalContext> related) {
        if (related == null || related.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n=====================================================================\n");
        sb.append("RELATED EXISTING WORK\n");
        sb.append("=====================================================================\n");
        sb.append("Goals this user has ALREADY planned that look related to what they are\n");
        sb.append("asking for now. Each task list below is COMPLETE for that goal — if a\n");
        sb.append("task is not listed, it does not exist.\n");

        for (RelatedGoalContext goal : related) {
            sb.append("\nGoal: \"").append(goal.title()).append("\"");
            if (goal.status() != null) {
                sb.append(" [").append(goal.status()).append("]");
            }
            if (goal.targetDate() != null) {
                sb.append(" target ").append(goal.targetDate());
            }
            sb.append("\n");
            if (goal.description() != null && !goal.description().isBlank()) {
                sb.append("  ").append(goal.description()).append("\n");
            }
            if (goal.tasks().isEmpty()) {
                sb.append("  (no tasks)\n");
                continue;
            }
            for (RelatedGoalContext.RelatedTask task : goal.tasks()) {
                sb.append("  ").append(task.completed() ? "[done]     " : "[not done] ")
                    .append(task.title());
                if (task.estimatedDuration() != null) {
                    sb.append(" (").append(task.estimatedDuration()).append(" min)");
                }
                sb.append("\n");
            }
            if (goal.omittedTaskCount() > 0) {
                sb.append("  ... and ").append(goal.omittedTaskCount())
                    .append(" more task(s) not shown\n");
            }
        }
        return sb.toString();
    }
}
