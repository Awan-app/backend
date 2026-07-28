package com.ezdo.timefold;

import ai.timefold.solver.core.api.solver.SolverManager;
import com.ezdo.exception.SchedulingFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class TimefoldSchedulerService {

    // Timefold 2.x: SolverManager has only ONE type parameter (no ProblemId_ generic)
    private final SolverManager<ScheduleSolution> solverManager;

    public ScheduleSolution schedule(ScheduleSolution problem) {
        try {
            // Timefold 2.x fluent solveBuilder API:
            // withProblem() directly provides the solution object (no separate problem finder)
            return solverManager.solveBuilder()
                    .withProblemId(problem.getGoalId())
                    .withProblem(problem)
                    .run()
                    .getFinalBestSolution();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchedulingFailedException(problem.getGoalId());
        }
    }
}
