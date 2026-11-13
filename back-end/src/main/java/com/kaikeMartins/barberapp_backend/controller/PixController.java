package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.PixResponse;
import com.kaikeMartins.barberapp_backend.service.PixService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos/pix")
@RequiredArgsConstructor
public class PixController {

    private final PixService pixService;

    @GetMapping("/agendamento/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PixResponse> gerar(@PathVariable Long id) {
        return ResponseEntity.ok(pixService.gerarPorAgendamento(id));
    }
}
