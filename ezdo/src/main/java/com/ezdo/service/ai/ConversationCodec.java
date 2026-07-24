package com.ezdo.service.ai;

import com.ezdo.dto.ai.decompose.BlockEnvelope;
import com.ezdo.dto.ai.decompose.ContentBlock;
import com.ezdo.dto.ai.decompose.ConversationMessage;
import com.ezdo.dto.ai.decompose.GoalProposal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * The single place that knows the JSON contract shared by three concerns:
 * persisting/loading the {@code messages} and {@code proposedGoal} columns of a
 * {@code GoalDecompositionSession}, and parsing the model's raw reply back into
 * polymorphic {@link ContentBlock}s. Wraps the Spring-Boot-autoconfigured
 * {@link ObjectMapper}; the {@code @JsonTypeInfo} discriminator on
 * {@link ContentBlock} round-trips natively.
 */
@Component
@RequiredArgsConstructor
public class ConversationCodec {

    private static final TypeReference<List<ConversationMessage>> TRANSCRIPT_TYPE =
        new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    /** Raised when the model's raw output cannot be parsed into blocks. */
    public static class BlockParseException extends RuntimeException {
        public BlockParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // --- transcript column ---------------------------------------------------

    public String writeTranscript(List<ConversationMessage> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize conversation transcript", e);
        }
    }

    public List<ConversationMessage> readTranscript(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, TRANSCRIPT_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize conversation transcript", e);
        }
    }

    // --- proposal column -----------------------------------------------------

    public String writeProposal(GoalProposal proposal) {
        try {
            return objectMapper.writeValueAsString(proposal);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize goal proposal", e);
        }
    }

    public GoalProposal readProposal(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, GoalProposal.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize goal proposal", e);
        }
    }

    // --- model output --------------------------------------------------------

    /** Serialize a set of blocks back to the {@code {"blocks":[...]}} envelope. */
    public String writeBlocks(List<ContentBlock> blocks) {
        try {
            return objectMapper.writeValueAsString(new BlockEnvelope(blocks));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize content blocks", e);
        }
    }

    /**
     * Defensive parse of the model's raw reply: strips markdown code fences and any
     * surrounding prose, then deserializes the {@code {"blocks":[...]}} envelope.
     *
     * @throws BlockParseException if no valid envelope can be extracted
     */
    public List<ContentBlock> parseBlocks(String rawModelOutput) {
        if (rawModelOutput == null || rawModelOutput.isBlank()) {
            throw new BlockParseException("Model returned empty output", null);
        }
        String json = rawModelOutput;
        try {
            BlockEnvelope envelope = objectMapper.readValue(json, BlockEnvelope.class);
            if (envelope.blocks() == null || envelope.blocks().isEmpty()) {
                throw new BlockParseException("Model output contained no blocks", null);
            }
            return envelope.blocks();
        } catch (BlockParseException e) {
            throw e;
        } catch (Exception e) {
            throw new BlockParseException("Model output was not a valid block envelope", e);
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
            throw new BlockParseException("No JSON object found in model output", null);
        }
        return s.substring(start, end + 1);
    }
}
