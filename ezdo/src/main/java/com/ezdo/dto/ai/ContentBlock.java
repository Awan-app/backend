package com.ezdo.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextBlock.class, name = "text"),
    @JsonSubTypes.Type(value = GoalProposalBlock.class, name = "proposal"),
    @JsonSubTypes.Type(value = QuestionBlock.class, name = "question")
})
public abstract class ContentBlock {

    // @JsonTypeInfo already writes the "type" discriminator; keep this getter out of
    // serialization so it isn't emitted a second time (which breaks round-tripping).
    @JsonIgnore
    public abstract String getType();
}
