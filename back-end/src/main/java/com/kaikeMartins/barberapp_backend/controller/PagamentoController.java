package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.dto.PagamentoResponse;
import com.kaikeMartins.barberapp_backend.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService service;

    public record MetodoRequest(PagamentoMetodo metodo) {}

    @GetMapping("/agendamentos/{agendamentoId}")
    public ResponseEntity<PagamentoResponse> consultar(@PathVariable Long agendamentoId) {
        return ResponseEntity.ok(service.consultar(agendamentoId));
    }

    @PutMapping("/agendamentos/{agendamentoId}/metodo")
    public ResponseEntity<PagamentoResponse> escolherMetodo(@PathVariable Long agendamentoId,
                                                            @RequestBody MetodoRequest request) {
        return ResponseEntity.ok(service.escolherMetodo(agendamentoId, request.metodo()));
    }

    @PostMapping("/agendamentos/{agendamentoId}/informar-pix")
    public ResponseEntity<PagamentoResponse> informarPix(@PathVariable Long agendamentoId) {
        return ResponseEntity.ok(service.informarPix(agendamentoId));
    }

    @PostMapping("/agendamentos/{agendamentoId}/confirmar")
    public ResponseEntity<PagamentoResponse> confirmar(@PathVariable Long agendamentoId) {
        return ResponseEntity.ok(service.confirmar(agendamentoId));
    }
}
