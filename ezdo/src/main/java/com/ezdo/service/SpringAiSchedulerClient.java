package com.ezdo.service;

import com.ezdo.dto.ai.schedule.AiSchedulingPayload;
import com.ezdo.dto.ai.schedule.AiSchedulingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SpringAiSchedulerClient implements AiSchedulerClient {

    private final ChatClient chatClient;
    private final String systemPrompt;

    public SpringAiSchedulerClient(
        @Qualifier("planningModel")
        ChatClient chatClient,
        @Value("classpath:prompts/scheduling-system.txt") Resource promptResource
    ) throws IOException {
        this.chatClient = chatClient;
        this.systemPrompt = new String(
                promptResource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    @Override
    public AiSchedulingResult scheduleTasks(AiSchedulingPayload payload) {
        try {
            return callModel(payload);
        } catch (RuntimeException first) {
            log.warn("AI scheduling returned an invalid response, retrying once", first);
            // second attempt — let any exception propagate to the caller
            return callModel(payload);
        }
    }

    // ── private ───────────────────────────────────────────────────────────────

    private AiSchedulingResult callModel(AiSchedulingPayload payload) {
        log.debug("Calling AI scheduler with payload for goal '{}'", payload.goalTitle());

        AiSchedulingResult result = chatClient.prompt()
                .system(systemPrompt)
                .user(user -> user
                        .text("""
                                Schedule the following tasks into the calendar based on the constraints.

                                {payload}
                                """)
                        .param("payload", payload)
                )
                .call()
                .entity(AiSchedulingResult.class);

        if (result == null) {
            throw new IllegalStateException("AI returned an empty (null) response for goal '"
                    + payload.goalTitle() + "'");
        }

        return result;
    }
}