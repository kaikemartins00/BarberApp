package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDateTime;

public record MensagemResponse(
        Long id,
        String texto,
        LocalDateTime enviadaEm,
        boolean minha,
        boolean lida
) {}
