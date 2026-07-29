package com.ezdo.service;

import com.ezdo.dto.ai.AiSchedulingPayload;
import com.ezdo.dto.ai.AiSchedulingResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class SpringAiSchedulerClient implements AiSchedulerClient {

    private final ChatClient chatClient;
    private final String systemPrompt;

    public SpringAiSchedulerClient(
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

        System.out.println("Scheduling tasks with payload: " + payload);

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
            throw new IllegalStateException("AI returned an empty response.");
        }

        return result;
    }
}