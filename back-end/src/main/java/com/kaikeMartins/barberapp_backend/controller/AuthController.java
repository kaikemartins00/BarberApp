package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/cadastro-barbeiro")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerBarbeiro(@Valid @RequestBody BarbeiroRequest request) {
        service.registerBarbeiro(request);
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody CadastroRequest request) {
        service.register(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/validar-codigo")
    @ResponseStatus(HttpStatus.OK)
    public void validarCodigo(@Valid @RequestBody ValidarCodigoRequest request) {
        service.validarCodigo(request);
    }

    @PostMapping("/reenviar-codigo")
    @ResponseStatus(HttpStatus.OK)
    public void reenviarCodigo(@RequestParam String email) {
        service.reenviarCodigo(email);
    }
}
