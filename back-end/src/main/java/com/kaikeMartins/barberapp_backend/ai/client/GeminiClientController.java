package com.kaikeMartins.barberapp_backend.ai.client;

import com.kaikeMartins.barberapp_backend.ai.dto.GeminiRequest;
import com.kaikeMartins.barberapp_backend.ai.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gemini")
@RequiredArgsConstructor
public class GeminiClientController {

    private final GeminiClient client;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<GeminiResponse> createGemini(@RequestBody GeminiRequest request){
        return client.getGeminiRequest(request);
    }
}
