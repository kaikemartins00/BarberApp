package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.domain.entities.ChamadoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.RespostaChamadoEntity;
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
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public List<ChamadoResponse> findAll() {
        return service.findAll();

    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public ChamadoEntity create(@Valid @RequestBody ChamadoRequest request) {
        return service.create(request);

    }

    @PostMapping("/responder")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public RespostaChamadoEntity resposta(@Valid @RequestBody Long id, RespostaChamRequest request) {
        return service.iniciarChamado(id, request);

    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public void delete(@PathVariable Long id) {
        service.finalizarChamado(id);

    }

}
