package com.ezdo.util;

import com.ezdo.dto.goal.DraftTaskRequest;
import com.ezdo.exception.TaskCyclicDependencyException;

import java.util.*;

public class DependencyGraphValidator {

    private DependencyGraphValidator() {}

    public static void assertNoCycles(List<DraftTaskRequest> tasks) {
        Map<String, List<String>> graph = new HashMap<>();
        for (DraftTaskRequest dt : tasks) {
            graph.put(dt.tempId(), Optional.ofNullable(dt.dependsOnTempIds()).orElse(List.of()));
        }
        assertNoCycles(graph);
    }

    public static void assertNoCycles(Map<String, List<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        for (String node : graph.keySet()) {
            if (hasCycle(node, graph, visited, inStack)) {
                throw new TaskCyclicDependencyException();
            }
        }
    }

    private static boolean hasCycle(String node, Map<String, List<String>> graph,
                                    Set<String> visited, Set<String> inStack) {
        if (inStack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        inStack.add(node);
        for (String next : graph.getOrDefault(node, List.of())) {
            if (hasCycle(next, graph, visited, inStack)) return true;
        }
        inStack.remove(node);
        return false;
    }
}
