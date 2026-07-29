package com.ezdo.service.ai.plan;

import com.ezdo.dto.ai.plan.TaskPlanResult;
import com.ezdo.exception.ResultParseException;
import com.ezdo.service.ai.JsonExtractor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * The single place that knows how to turn the planning model's raw reply into a
 * {@link TaskPlanResult}. Wraps the Spring-Boot-autoconfigured {@link ObjectMapper}
 * and strips markdown fences/surrounding prose via {@link JsonExtractor} first —
 * the contract forbids fences, but models emit them often enough that failing on
 * one would waste a retry on an otherwise perfect reply.
 */
@Component
public class TaskPlanningCodec {

    private final ObjectMapper objectMapper;

    public TaskPlanningCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TaskPlanResult parseResult(String rawModelOutput) {
        String json;
        try {
            json = JsonExtractor.extractObject(rawModelOutput);
        } catch (JsonExtractor.NoJsonObjectException e) {
            throw new ResultParseException(e.getMessage(), e);
        }
        try {
            return objectMapper.readValue(json, TaskPlanResult.class);
        } catch (Exception e) {
            throw new ResultParseException("Model output was not a valid task plan", e);
        }
    }
}
