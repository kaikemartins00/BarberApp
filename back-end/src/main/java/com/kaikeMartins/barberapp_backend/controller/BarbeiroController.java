package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroRequest;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroUpdateRequest;
import com.kaikeMartins.barberapp_backend.dto.HorariosRequest;
import com.kaikeMartins.barberapp_backend.dto.ServicosIdsRequest;
import com.kaikeMartins.barberapp_backend.service.BarbeiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService service;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public List<BarbeiroEntity> findAll() {
        return service.findAll();

    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public BarbeiroEntity findById(@PathVariable Long id) {
        return service.findById(id);

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public BarbeiroEntity create(@Valid @RequestBody BarbeiroRequest request) {
        return service.create(request);

    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public BarbeiroEntity update(@PathVariable Long id, @Valid @RequestBody BarbeiroUpdateRequest request) {
        return service.update(id, request);

    }

    @GetMapping("/{id}/servicos")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public Set<ServicoEntity> servicosDoBarbeiro(@PathVariable Long id) {
        return service.findById(id).getServicos();

    }

    @GetMapping("/{id}/horarios")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public Set<LocalTime> horariosDoBarbeiro(@PathVariable Long id) {
        return service.findById(id).getHorariosTrabalho();
    }

    @PutMapping("/me/horarios")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public void atualizarHorarios(@RequestBody HorariosRequest request) {
        service.atualizarHorarios(request);
    }

    @PutMapping("/me/servicos")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public void atualizarServicos(@RequestBody ServicosIdsRequest request) {
        service.atualizarServicos(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}