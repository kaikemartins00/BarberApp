package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.ConversaRequest;
import com.kaikeMartins.barberapp_backend.dto.ConversaResponse;
import com.kaikeMartins.barberapp_backend.dto.ClienteConversaResponse;
import com.kaikeMartins.barberapp_backend.dto.MensagemRequest;
import com.kaikeMartins.barberapp_backend.dto.MensagemResponse;
import com.kaikeMartins.barberapp_backend.service.ConversaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversas")
@RequiredArgsConstructor
public class ConversaController {

    private final ConversaService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO')")
    public List<ConversaResponse> minhasConversas() {
        return service.minhasConversas();
    }

    @GetMapping("/clientes-disponiveis")
    @PreAuthorize("hasRole('BARBEIRO')")
    public List<ClienteConversaResponse> clientesDisponiveis() {
        return service.clientesDisponiveis();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO')")
    public ConversaResponse iniciar(@Valid @RequestBody ConversaRequest request) {
        return service.iniciar(request);
    }

    @GetMapping("/{id}/mensagens")
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO')")
    public List<MensagemResponse> mensagens(@PathVariable Long id,
                                            @RequestParam(required = false) Long depois) {
        return service.mensagens(id, depois);
    }

    @PostMapping("/{id}/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO')")
    public MensagemResponse enviar(@PathVariable Long id, @Valid @RequestBody MensagemRequest request) {
        return service.enviar(id, request);
    }

    @PutMapping("/{id}/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO')")
    public void marcarComoLidas(@PathVariable Long id) {
        service.marcarComoLidas(id);
    }

    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO')")
    public ConversaResponse finalizar(@PathVariable Long id) {
        return service.finalizar(id);
    }

    @PutMapping("/{id}/reabrir")
    @PreAuthorize("hasRole('CLIENTE')")
    public ConversaResponse reabrir(@PathVariable Long id) {
        return service.reabrir(id);
    }
}