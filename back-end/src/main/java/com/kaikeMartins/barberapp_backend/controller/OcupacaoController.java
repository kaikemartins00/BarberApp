package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.OcupacaoResponse;
import com.kaikeMartins.barberapp_backend.service.OcupacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/ocupacao")
@RequiredArgsConstructor
public class OcupacaoController {

    private final OcupacaoService service;

    @GetMapping("/barbeiros/{barbeiroId}")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'ADMIN')")
    public OcupacaoResponse consultar(@PathVariable Long barbeiroId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.consultar(barbeiroId, data);
    }
}
