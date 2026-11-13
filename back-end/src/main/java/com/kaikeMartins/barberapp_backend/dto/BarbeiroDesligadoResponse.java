package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDateTime;

public record BarbeiroDesligadoResponse(
        Long id,
        String nome,
        String email,
        LocalDateTime desligadoEm,
        String motivo
) {}
