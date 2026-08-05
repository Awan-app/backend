package com.ezdo.service.ai.decompose;

import com.ezdo.dto.ai.decompose.BlockEnvelope;
import com.ezdo.dto.ai.decompose.ContentBlock;
import com.ezdo.dto.ai.decompose.ConversationMessage;
import com.ezdo.dto.ai.decompose.GoalProposal;
import com.ezdo.exception.ResultParseException;
import com.ezdo.service.ai.JsonExtractor;
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
     * @throws ResultParseException if no valid envelope can be extracted
     */
    public List<ContentBlock> parseBlocks(String rawModelOutput) {
        String json;
        try {
            json = JsonExtractor.extractObject(rawModelOutput);
        } catch (JsonExtractor.NoJsonObjectException e) {
            throw new ResultParseException(e.getMessage(), e);
        }
        try {
            BlockEnvelope envelope = objectMapper.readValue(json, BlockEnvelope.class);
            if (envelope.blocks() == null || envelope.blocks().isEmpty()) {
                throw new ResultParseException("Model output contained no blocks", null);
            }
            return envelope.blocks();
        } catch (ResultParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResultParseException("Model output was not a valid block envelope", e);
        }
    }
}
