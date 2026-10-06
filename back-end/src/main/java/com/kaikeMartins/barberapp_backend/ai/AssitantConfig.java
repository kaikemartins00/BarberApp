package com.kaikeMartins.barberapp_backend.ai;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssitantConfig {
    @Value("${gemini.api_key}")
    private String apiKey;
    @Value("${gemini.url}")
    private String geminiUrl;

    @Bean
    public GoogleAiGeminiChatModel googleAiGeminiChatModel(){
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(geminiUrl)
                .returnThinking(true)
                .sendThinking(true)
                .build();
    }

    @Bean
    public AssistantAiService assistant(GoogleAiGeminiChatModel model, AssistantTools assistantTools){
        return AiServices.builder(AssistantAiService.class)
                .chatModel(model)
                .tools(assistantTools)
                .build();
    }
}
