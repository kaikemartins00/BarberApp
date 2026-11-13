package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.NotaAgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.PreferenciasClienteRequest;
import com.kaikeMartins.barberapp_backend.dto.PreferenciasClienteResponse;
import com.kaikeMartins.barberapp_backend.dto.ServicoPopularResponse;
import com.kaikeMartins.barberapp_backend.service.PainelClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cliente/painel")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class PainelClienteController {

    private final PainelClienteService service;

    @GetMapping("/avaliacoes")
    public List<NotaAgendamentoResponse> avaliacoes() {
        return service.notasDoCliente();
    }

    @GetMapping("/servicos-populares")
    public List<ServicoPopularResponse> servicosPopulares() {
        return service.servicosMaisFeitos();
    }

    @GetMapping("/preferencias")
    public PreferenciasClienteResponse preferencias() {
        return service.preferencias();
    }

    @PutMapping("/preferencias")
    public PreferenciasClienteResponse salvarPreferencias(@Valid @RequestBody PreferenciasClienteRequest request) {
        return service.salvarPreferencias(request);
    }
}
