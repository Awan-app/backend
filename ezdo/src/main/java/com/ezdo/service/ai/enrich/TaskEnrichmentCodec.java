package com.ezdo.service.ai.enrich;

import com.ezdo.dto.ai.enrich.TaskEnrichmentResult;
import com.ezdo.exception.ResultParseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * The single place that knows how to turn the model's raw reply into a
 * {@link TaskEnrichmentResult}. Wraps the Spring-Boot-autoconfigured
 * {@link ObjectMapper}, defensively stripping markdown fences/surrounding prose
 * the same way {@code ConversationCodec} does for the decomposition contract.
 */
@Component
public class TaskEnrichmentCodec {

    private final ObjectMapper objectMapper;

    public TaskEnrichmentCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TaskEnrichmentResult parseResult(String rawModelOutput) {
        if (rawModelOutput == null || rawModelOutput.isBlank()) {
            throw new ResultParseException("Model returned empty output", null);
        }
        try {
            return objectMapper.readValue(rawModelOutput, TaskEnrichmentResult.class);
        } catch (Exception e) {
            throw new ResultParseException("Model output was not a valid enrichment result", e);
        }
    }
}