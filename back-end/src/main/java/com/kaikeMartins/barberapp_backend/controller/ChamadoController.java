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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService service;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ChamadoResponse> findAll() {

        return service.findAll();

    }

    @PostMapping("/criar")
    @ResponseStatus(HttpStatus.CREATED)
    public ChamadoEntity create(@Valid @RequestBody ChamadoRequest request) {
        return service.create(request);

    }

    @PostMapping("/responder")
    @ResponseStatus(HttpStatus.CREATED)
    public RespostaChamadoEntity resposta(@Valid @RequestBody Long id, RespostaChamRequest request) {
        return service.iniciarChamado(id, request);

    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.finalizarChamado(id);
    }

}
