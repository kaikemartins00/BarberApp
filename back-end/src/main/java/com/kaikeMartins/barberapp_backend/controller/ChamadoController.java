package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.ChamadoRequest;
import com.kaikeMartins.barberapp_backend.dto.ChamadoResponse;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamRequest;
import com.kaikeMartins.barberapp_backend.service.ChamadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public List<ChamadoResponse> findAll() { return service.findAll(); }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<ChamadoResponse> meus() { return service.meus(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENTE')")
    public ChamadoResponse create(@Valid @RequestBody ChamadoRequest request) { return service.create(request); }

    @PostMapping("/{id}/assumir")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public ChamadoResponse assumir(@PathVariable Long id) { return service.assumir(id); }

    @PostMapping("/{id}/respostas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public ChamadoResponse responder(@PathVariable Long id, @Valid @RequestBody RespostaChamRequest request) {
        return service.responder(id, request);
    }

    @PutMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public ChamadoResponse finalizar(@PathVariable Long id) { return service.finalizar(id); }
}