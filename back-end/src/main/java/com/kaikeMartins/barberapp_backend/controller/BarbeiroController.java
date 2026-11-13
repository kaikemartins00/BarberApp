package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.service.BarbeiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public List<BarbeiroResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'BARBEIRO', 'ADMIN')")
    public BarbeiroResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BARBEIRO')")
    public BarbeiroResponse update(@PathVariable Long id, @Valid @RequestBody BarbeiroUpdateRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/{id}/servicos")
    @PreAuthorize("hasRole('ADMIN')")
    public BarbeiroResponse atualizarServicos(@PathVariable Long id, @Valid @RequestBody ServicosIdsRequest request) {
        return service.atualizarServicos(id, request);
    }

    @PutMapping("/{id}/horarios")
    @PreAuthorize("hasRole('ADMIN')")
    public BarbeiroResponse atualizarHorarios(@PathVariable Long id, @Valid @RequestBody HorariosRequest request) {
        return service.atualizarHorarios(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { service.delete(id); }

    @GetMapping("/desligados")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BarbeiroDesligadoResponse> desligados() { return service.listarDesligados(); }

    @PostMapping("/{id}/desligar")
    @PreAuthorize("hasRole('ADMIN')")
    public CancelamentoResponse desligar(@PathVariable Long id, @Valid @RequestBody DesligarBarbeiroRequest request) {
        return service.desligar(id, request);
    }

    @PostMapping("/{id}/cancelar-dia")
    @PreAuthorize("hasRole('ADMIN')")
    public CancelamentoResponse cancelarDia(@PathVariable Long id, @Valid @RequestBody CancelarDiaRequest request) {
        return service.cancelarDia(id, request);
    }
}
