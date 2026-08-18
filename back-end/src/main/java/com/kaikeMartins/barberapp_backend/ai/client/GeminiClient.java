package com.kaikeMartins.barberapp_backend.ai.client;

import com.kaikeMartins.barberapp_backend.ai.dto.GeminiContent;
import com.kaikeMartins.barberapp_backend.ai.dto.GeminiPart;
import com.kaikeMartins.barberapp_backend.ai.dto.GeminiRequest;
import com.kaikeMartins.barberapp_backend.ai.dto.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;

    public GeminiClient(@Value("${gemini.url}") String url,
                        @Value("${gemini.api-key}") String apiKey) {
        this.restClient = RestClient.create(url);
        this.apiKey = apiKey;
    }

    public String enviarMensagem(String pergunta) {

        GeminiRequest request = new GeminiRequest(
                List.of(
                        new GeminiContent(
                                List.of(
                                        new GeminiPart(pergunta)
                                )
                        )
                )
        );

        GeminiResponse response = restClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        return response.candidates().get(0).content().parts().get(0).text();
    }
}