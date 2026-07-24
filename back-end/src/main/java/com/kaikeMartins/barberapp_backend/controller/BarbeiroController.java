package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroRequest;
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
    public List<BarbeiroEntity> findAll() {

        return service.findAll();

    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BarbeiroEntity findById(@PathVariable Long id) {
        return service.findById(id);

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BarbeiroEntity create(@Valid @RequestBody BarbeiroRequest request) {
        return service.create(request);

    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BarbeiroEntity update(@PathVariable Long id, @Valid @RequestBody BarbeiroRequest request) {
        return service.update(id, request);

    }

    @GetMapping("/{id}/servicos")
    @ResponseStatus(HttpStatus.OK)
    public Set<ServicoEntity> servicosDoBarbeiro(@PathVariable Long id) {
        return service.findById(id).getServicos();
    }

    @GetMapping("/{id}/horarios")
    @ResponseStatus(HttpStatus.OK)
    public Set<LocalTime> horariosDoBarbeiro(@PathVariable Long id) {
        return service.findById(id).getHorariosTrabalho();
    }

    @PutMapping("/me/horarios")
    @PreAuthorize("hasRole('BARBEIRO')")
    @ResponseStatus(HttpStatus.OK)
    public void atualizarHorarios(@RequestBody HorariosRequest request) {
        service.atualizarHorarios(request);
    }

    @PutMapping("/me/servicos")
    @PreAuthorize("hasRole('BARBEIRO')")
    @ResponseStatus(HttpStatus.OK)
    public void atualizarServicos(@RequestBody ServicosIdsRequest request) {
        service.atualizarServicos(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}