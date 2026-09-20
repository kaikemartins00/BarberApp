package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService service;

    @GetMapping("/meus")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public List<AgendamentoResponse> meusAgendamentos() {
        return service.meusAgendamentos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public AgendamentoResponse create(@Valid @RequestBody AgendamentoRequest request) {
        return service.create(request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public List<AgendamentoResponse> findAll() {
        return service.findAll();
    }

    @PutMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public AgendamentoResponse atualizarStatus(@PathVariable Long id, @RequestParam String status) {
        return service.atualizarStatus(id, status);
    }
}