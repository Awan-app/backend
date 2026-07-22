package com.ezdo.mapper;

import com.ezdo.dto.ai.GoalProposal;
import com.ezdo.dto.ai.TaskProposal;
import com.ezdo.dto.goal.DraftTaskRequest;
import com.ezdo.dto.goal.GoalCreateRequest;
import com.ezdo.exception.InvalidDecompositionException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Maps an AI {@link GoalProposal} onto the existing {@link GoalCreateRequest} so
 * confirmation reuses {@code GoalService.createGoal} (and its cycle validation).
 * Because that path is invoked programmatically, bean-validation on the request
 * record does not fire here, so this mapper guards the invariants createGoal
 * assumes and rejects a malformed proposal with {@link InvalidDecompositionException}.
 */
@Component
public class ProposalMapper {

    private static final int MAX_TASKS = 50;

    public GoalCreateRequest toCreateRequest(GoalProposal proposal) {
        if (proposal == null) {
            throw new InvalidDecompositionException("No goal proposal to confirm");
        }
        if (proposal.title() == null || proposal.title().isBlank()) {
            throw new InvalidDecompositionException("Proposed goal is missing a title");
        }

        List<TaskProposal> tasks = proposal.tasks() != null ? proposal.tasks() : List.of();
        if (tasks.size() > MAX_TASKS) {
            throw new InvalidDecompositionException(
                "Proposed goal has " + tasks.size() + " tasks; the maximum is " + MAX_TASKS);
        }

        List<DraftTaskRequest> drafts = tasks.stream()
            .map(this::toDraft)
            .toList();

        return new GoalCreateRequest(
            proposal.title().strip(),
            proposal.description(),
            parseTargetDate(proposal.targetDate()),
            drafts
        );
    }

    private DraftTaskRequest toDraft(TaskProposal t) {
        if (t.title() == null || t.title().isBlank()) {
            throw new InvalidDecompositionException("A proposed task is missing a title");
        }
        if (t.tempId() == null || t.tempId().isBlank()) {
            throw new InvalidDecompositionException("A proposed task is missing a tempId");
        }
        return new DraftTaskRequest(
            t.tempId(),
            t.title().strip(),
            t.description(),
            t.estimatedDuration(),
            t.mandatory(),
            t.estimatedPoints(),
            t.allowTaskSplitting(),
            t.dependsOnTempIds()
        );
    }

    /** Tolerant parse: null/blank/unparseable targetDate becomes null rather than a 500. */
    private LocalDate parseTargetDate(String raw) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw.strip())) {
            return null;
        }
        try {
            return LocalDate.parse(raw.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
