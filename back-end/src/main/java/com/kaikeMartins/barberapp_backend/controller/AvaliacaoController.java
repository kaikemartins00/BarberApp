package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {
    private final AvaliacaoService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('CLIENTE')")
    public AvaliacaoResponse criar(@Valid @RequestBody AvaliacaoRequest r) {
        return service.criar(r);
    }

    @GetMapping("/agendamentos/{id}") @PreAuthorize("isAuthenticated()")
    public java.util.Map<String,Boolean> porAgendamento(@PathVariable Long id) {
        return java.util.Map.of("avaliado", service.porAgendamento(id));
    }
    @GetMapping("/barbeiros/{id}/resumo") @PreAuthorize("isAuthenticated()")
    public AvaliacaoResumoResponse resumo(@PathVariable Long id) {
        return service.resumo(id);
    }
}
