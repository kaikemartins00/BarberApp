package com.kaikeMartins.barberapp_backend.ai.client;

import com.kaikeMartins.barberapp_backend.ai.dto.GeminiRequest;
import com.kaikeMartins.barberapp_backend.ai.dto.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.Duration;

import static org.springframework.http.MediaType.APPLICATION_JSON;


@Component
public class GeminiClient {

    private RestClient customClient;

    private String geminiUrl;

    private String apiKey;

    private String nome = "x-goog-api-key";

    public GeminiClient(@Value("${gemini.api_key}") String apiKey, @Value("${gemini.url}") String geminiUrl) {
        this.apiKey = apiKey;
        this.geminiUrl = geminiUrl;

        Duration timeout = Duration.ofSeconds(30);

        JdkClientHttpRequestFactory  jdkClientHttpRequestFactory = new JdkClientHttpRequestFactory();
        jdkClientHttpRequestFactory.setReadTimeout(timeout);

        this.customClient = RestClient.builder()
                .baseUrl(geminiUrl)
                .defaultHeader(nome, apiKey)
                .requestFactory(jdkClientHttpRequestFactory)
                .build();

        }
    public ResponseEntity<GeminiResponse> getGeminiRequest(GeminiRequest geminiRequest) {
        ResponseEntity<GeminiResponse> request = customClient.post()
                .contentType(APPLICATION_JSON)
                .body(geminiRequest)
                .retrieve()
                .toEntity(GeminiResponse.class);

        return request;
    }

}