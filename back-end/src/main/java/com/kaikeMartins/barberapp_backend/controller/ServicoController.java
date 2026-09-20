package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.dto.ServicoRequest;
import com.kaikeMartins.barberapp_backend.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService service;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public List<ServicoEntity> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public ServicoEntity findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public ServicoEntity create(@Valid @RequestBody ServicoRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public ServicoEntity update(@PathVariable Long id, @Valid @RequestBody ServicoRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
