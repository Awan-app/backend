package com.ezdo.dto;

import java.util.UUID;

public record RelatedGoalMatch(
        UUID goalId,
        String text,
        Double score
) {
}
