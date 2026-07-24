package com.ezdo.service.ai;

import com.ezdo.dto.ai.decompose.*;
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

    public List<Message> build(List<ConversationMessage> transcript, DecompositionUserContext context) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt + renderUserContext(context)));
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

    /**
     * Renders the per-user grounding section referenced by the system prompt's
     * "USER CONTEXT" pointers (categories, targetDate estimation, task sizing).
     * Appended to the static prompt rather than sent as a separate SystemMessage.
     */
    private String renderUserContext(DecompositionUserContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n=====================================================================\n");
        sb.append("USER CONTEXT\n");
        sb.append("=====================================================================\n");
        sb.append("Today's date: ").append(ctx.today()).append("\n");

        if (ctx.timezone() != null) {
            sb.append("User's timezone: ").append(ctx.timezone()).append("\n");
        }
        if (ctx.preferredSessionDuration() != null) {
            sb.append("User's preferred focused-session length: ")
                .append(ctx.preferredSessionDuration()).append(" minutes. ")
                .append("Prefer sizing individual task durations around this rather than the generic ")
                .append("15-minutes-to-half-a-day range where the two conflict.\n");
        }
        if (ctx.bufferBetweenSessions() != null) {
            sb.append("User's preferred buffer between sessions: ")
                .append(ctx.bufferBetweenSessions()).append(" minutes.\n");
        }

        sb.append("Categories — use the EXACT id and name below for a task's \"category\", ")
            .append("or null if none fit. Never invent an id or name not listed here:\n");
        if (ctx.categories() == null || ctx.categories().isEmpty()) {
            sb.append("- (this user has no categories yet; every task's category must be null)\n");
        } else {
            for (DecompositionUserContext.CategoryOption c : ctx.categories()) {
                sb.append("- id: ").append(c.id()).append(", name: \"").append(c.name()).append("\"\n");
            }
        }
        return sb.toString();
    }
}
