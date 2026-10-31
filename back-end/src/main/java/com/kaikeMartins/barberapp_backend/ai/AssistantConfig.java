package com.kaikeMartins.barberapp_backend.ai;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantConfig {
    @Value("${gemini.api_key}")
    private String apiKey;
    @Value("${gemini.url}")
    private String geminiUrl;

    @Bean
    public GoogleAiGeminiChatModel googleAiGeminiChatModel(){
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(geminiUrl)
                .maxRetries(1)
                .returnThinking(true)
                .sendThinking(true)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.withMaxMessages(20);
    }
}
