package com.ezdo.dto.ai;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextBlock.class, name = "text"),
    @JsonSubTypes.Type(value = GoalProposalBlock.class, name = "proposal"),
    @JsonSubTypes.Type(value = QuestionBlock.class, name = "question")
})
public abstract class ContentBlock {
    public abstract String getType();
}
