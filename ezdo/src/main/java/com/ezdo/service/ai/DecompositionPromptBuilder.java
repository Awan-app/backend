package com.ezdo.service.ai;

import com.ezdo.dto.ai.ContentBlock;
import com.ezdo.dto.ai.ConversationMessage;
import com.ezdo.dto.ai.QuestionBlock;
import com.ezdo.dto.ai.TextBlock;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a stored decomposition transcript into the ordered list of Spring AI
 * {@link Message}s sent to the model: a leading {@link SystemMessage} with the
 * decomposition contract, then each prior turn as a {@link UserMessage} or an
 * {@link AssistantMessage}. Assistant turns are replayed as their block-envelope
 * JSON so the model sees its own earlier questions and proposal verbatim.
 */
@Component
public class DecompositionPromptBuilder {

    private final String systemPrompt;
    private final ConversationCodec codec;

    public DecompositionPromptBuilder(
        @Value("classpath:prompts/goal-decomposition-system.txt")
        Resource systemPromptResource,
        ConversationCodec codec
    ) {
        this.codec = codec;
        try {
            this.systemPrompt = StreamUtils.copyToString(
                systemPromptResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load goal-decomposition system prompt", e);
        }
    }

    public List<Message> build(List<ConversationMessage> transcript) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        for (ConversationMessage turn : transcript) {
            if ("assistant".equalsIgnoreCase(turn.role())) {
                messages.add(new AssistantMessage(codec.writeBlocks(turn.blocks())));
            } else {
                messages.add(new UserMessage(plainText(turn.blocks())));
            }
        }
        return messages;
    }

    /** Flatten a user turn's blocks to plain text for the model. */
    private String plainText(List<ContentBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : blocks) {
            if (block instanceof TextBlock t) {
                sb.append(t.getText());
            } else if (block instanceof QuestionBlock q) {
                sb.append(q.getText());
            }
            sb.append('\n');
        }
        return sb.toString().strip();
    }
}
