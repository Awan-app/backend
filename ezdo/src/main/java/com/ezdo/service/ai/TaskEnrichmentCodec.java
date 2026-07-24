package com.ezdo.service.ai;

import com.ezdo.dto.ai.enrich.TaskEnrichmentResult;
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

    /** Raised when the model's raw output cannot be parsed into a result. */
    public static class ResultParseException extends RuntimeException {
        public ResultParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

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

    private String extractJsonObject(String raw) {
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline >= 0) {
                s = s.substring(firstNewline + 1);
            }
            int closingFence = s.lastIndexOf("```");
            if (closingFence >= 0) {
                s = s.substring(0, closingFence);
            }
            s = s.strip();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new ResultParseException("No JSON object found in model output", null);
        }
        return s.substring(start, end + 1);
    }
}