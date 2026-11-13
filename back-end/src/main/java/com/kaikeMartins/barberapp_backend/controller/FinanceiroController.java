package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.FinanceiroResumoResponse;
import com.kaikeMartins.barberapp_backend.service.FinanceiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroService service;

    @GetMapping("/barbeiro/meu")
    @PreAuthorize("hasRole('BARBEIRO')")
    public FinanceiroResumoResponse meu() {
        return service.meu();
    }

    @GetMapping("/admin/resumo")
    @PreAuthorize("hasRole('ADMIN')")
    public FinanceiroResumoResponse geral() {
        return service.geral();
    }
}
