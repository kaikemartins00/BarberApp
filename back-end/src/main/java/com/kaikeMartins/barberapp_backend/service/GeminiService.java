package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.ai.client.GeminiClient;
import com.kaikeMartins.barberapp_backend.ai.dto.GeminiRequest;
import com.kaikeMartins.barberapp_backend.ai.dto.GeminiResponse;
import com.google.genai.Client;
import com.google.genai.gaos.models.interactions.CreateModelInteraction;
import com.google.genai.gaos.models.interactions.Interaction;
import com.google.genai.gaos.models.interactions.InteractionsInput;
import com.google.genai.gaos.models.interactions.Model;
import com.google.genai.gaos.models.operations.CreateInteractionRequestBody;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.HistoricoConversaEntity;
import com.kaikeMartins.barberapp_backend.dto.HistoricoResponse;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.HistoricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final HistoricoRepository historicoRepository;
    private final GeminiClient geminiClient;
    private final ClienteRepository clienteRepository;
    private GeminiResponse geminiResponse;

    public HistoricoResponse toResponse(HistoricoConversaEntity entity) {
        return new HistoricoResponse(
                entity.getId(),
                entity.getRole(),
                entity.getMsg(),
                entity.getMsgDate(),
                entity.getCliente()
        );
    }

    public List<HistoricoConversaEntity> findAll() {
            return historicoRepository.findAll();
    }

    public GeminiResponse conversa(GeminiRequest request, HistoricoResponse response) {
        ClienteEntity cliente = getAuthenticatedUser();

        ResponseEntity resp = geminiClient.getGeminiRequest(request);

        Client client = new Client();

        CreateModelInteraction params =
                CreateModelInteraction.builder()
                        .model(Model.of(""))
                        .input(InteractionsInput.of(""))
                        .build();

        Interaction interaction =
                client.interactions.create(CreateInteractionRequestBody.of(params)).interaction().get();

        HistoricoConversaEntity entity = HistoricoConversaEntity.builder()
                .id(response.id())
                .role(response.role())
                .msg(String.valueOf(resp))
                .msgDate(response.msgDate())
                .cliente(cliente)
                .build();

        historicoRepository.save(entity);
        return geminiResponse;

    }

    private ClienteEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return clienteRepository.findByEmail(authentication.getName()).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
