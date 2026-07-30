package com.ezdo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean("visionModel")
    public ChatClient googleGenAiChatClient(
        @Qualifier("googleGenAiChatModel") ChatModel googleGenAiChatModel
    ) {
        return ChatClient.builder(googleGenAiChatModel).build();
    }

    @Bean("planningModel")
    public ChatClient openAiChatClient(
        @Qualifier("googleGenAiChatModel") ChatModel openAiChatModel
    ) {
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean("planningModel2")
    public ChatClient openAiChatClient2(
        @Qualifier("openAiChatModel") ChatModel openAiChatModel
    ) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
