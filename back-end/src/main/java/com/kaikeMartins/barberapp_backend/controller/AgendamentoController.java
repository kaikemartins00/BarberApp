package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.ResumoCancelamentosResponse;
import com.kaikeMartins.barberapp_backend.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService service;

    @GetMapping("/meus")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public List<AgendamentoResponse> meusAgendamentos() {
        return service.meusAgendamentos();
    }

    @GetMapping("/meus/cancelamentos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResumoCancelamentosResponse cancelamentos() {
        return service.resumoCancelamentos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public AgendamentoResponse create(@Valid @RequestBody AgendamentoRequest request) {
        return service.create(request);
    }

    @GetMapping("/horarios-livres")
    @PreAuthorize("isAuthenticated()")
    public List<LocalTime> horariosLivres(@RequestParam Long barbeiroId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.horariosLivres(barbeiroId, data);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public List<AgendamentoResponse> findAll() {
        return service.findAll();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public AgendamentoResponse atualizarStatus(@PathVariable Long id, @RequestParam AgendamentoStatus status) {
        return service.atualizarStatus(id, status);
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public AgendamentoResponse concluir(@PathVariable Long id) {
        return service.registrarComparecimento(id, AgendamentoStatus.CONCLUIDO
        );
    }

    @PatchMapping("/{id}/nao-compareceu")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public AgendamentoResponse registrarFalta(@PathVariable Long id) {
        return service.registrarComparecimento(id, AgendamentoStatus.NAO_COMPARECEU);
    }

}