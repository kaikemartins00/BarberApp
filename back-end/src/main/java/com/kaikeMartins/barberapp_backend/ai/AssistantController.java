package com.kaikeMartins.barberapp_backend.ai;

import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.HistoricoConversaEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.dto.ChatRequest;
import com.kaikeMartins.barberapp_backend.dto.ChatResponse;
import com.kaikeMartins.barberapp_backend.dto.HistoricoResponse;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.HistoricoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantAiService assistantAiService;
    private final HistoricoRepository historicoRepository;
    private final ClienteRepository clienteRepository;

    private ClienteEntity cliente(UsuarioEntity usuario) {
        return clienteRepository.findById(usuario.getId())
                .orElseThrow(() -> new ForbiddenException("Apenas clientes podem usar o assistente."));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ChatResponse ask(@AuthenticationPrincipal UsuarioEntity usuario,
                            @Valid @RequestBody ChatRequest request) {
        ClienteEntity cliente = cliente(usuario);

        LocalDateTime perguntouEm = LocalDateTime.now();
        String resposta = assistantAiService.chat(cliente.getId(), request.mensagem());

        historicoRepository.save(HistoricoConversaEntity.builder()
                .role("user").msg(request.mensagem()).msgDate(perguntouEm).cliente(cliente).build());
        historicoRepository.save(HistoricoConversaEntity.builder()
                .role("assistant").msg(resposta).msgDate(LocalDateTime.now()).cliente(cliente).build());

        return new ChatResponse(resposta);
    }

    @GetMapping("/historico")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<HistoricoResponse> historico(@AuthenticationPrincipal UsuarioEntity usuario) {
        return historicoRepository.findByClienteOrderByMsgDateAsc(cliente(usuario)).stream()
                .map(h -> new HistoricoResponse(h.getId(), h.getRole(), h.getMsg(), h.getMsgDate()))
                .toList();
    }
}