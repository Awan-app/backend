package com.ezdo.service.ai.enrich;

import com.ezdo.dto.ai.AiUserPreferencesContext;
import com.ezdo.dto.ai.enrich.TaskEnrichmentRequest;
import com.ezdo.service.ai.UserContextRenderer;
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
import java.util.List;

@Component
public class TaskEnrichmentPromptBuilder {

    private final String systemPrompt;

    public TaskEnrichmentPromptBuilder(
        @Value("classpath:prompts/task-enrichment-system.txt")
        Resource systemPromptResource
    ) {
        try {
            this.systemPrompt = StreamUtils.copyToString(
                systemPromptResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load task-enrichment system prompt", e);
        }
    }

    public List<Message> build(TaskEnrichmentRequest request, AiUserPreferencesContext context) {
        return List.of(
            new SystemMessage(systemPrompt + UserContextRenderer.render(context)),
            new UserMessage(renderTask(request))
        );
    }

    private String renderTask(TaskEnrichmentRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Task title: ").append(request.title()).append("\n");
        if (request.description() != null && !request.description().isBlank()) {
            sb.append("Task description: ").append(request.description()).append("\n");
        } else {
            sb.append("Task description: (none provided — infer solely from the title)\n");
        }
        return sb.toString();
    }
}
