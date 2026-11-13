package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.FilaEsperaRequest;
import com.kaikeMartins.barberapp_backend.dto.FilaEsperaResponse;
import com.kaikeMartins.barberapp_backend.service.FilaEsperaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fila-espera")
@RequiredArgsConstructor
public class FilaEsperaController {

    private final FilaEsperaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENTE')")
    public FilaEsperaResponse entrar(@Valid @RequestBody FilaEsperaRequest request) {
        return service.entrar(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public FilaEsperaResponse consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    public FilaEsperaResponse sair(@PathVariable Long id) {
        return service.sair(id);
    }
}
