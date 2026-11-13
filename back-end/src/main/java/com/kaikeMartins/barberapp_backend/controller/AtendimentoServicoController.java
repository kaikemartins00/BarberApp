package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.service.AtendimentoServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agendamentos/{agendamentoId}/servicos")
@RequiredArgsConstructor
public class AtendimentoServicoController {

    private final AtendimentoServicoService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ContaAtendimentoResponse consultar(@PathVariable Long agendamentoId) {
        return service.consultar(agendamentoId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public ContaAtendimentoResponse adicionar(@PathVariable Long agendamentoId, @Valid @RequestBody AdicionarServicoAtendimentoRequest request) {
        return service.adicionar(agendamentoId, request);
    }

    @PatchMapping("/{itemId}/valor")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public ContaAtendimentoResponse alterarValor(@PathVariable Long agendamentoId, @PathVariable Long itemId, @Valid @RequestBody AlterarValorServicoRequest request) {
        return service.alterarValor(agendamentoId, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public ContaAtendimentoResponse remover(@PathVariable Long agendamentoId, @PathVariable Long itemId) {
        return service.removerExtra(agendamentoId, itemId);
    }
}
