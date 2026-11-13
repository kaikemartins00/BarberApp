package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.CheckoutRequest;
import com.kaikeMartins.barberapp_backend.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agendamentos/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutService checkoutService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public AgendamentoResponse finalizar(@Valid @RequestBody CheckoutRequest request) {
        return checkoutService.finalizar(request);
    }
}
