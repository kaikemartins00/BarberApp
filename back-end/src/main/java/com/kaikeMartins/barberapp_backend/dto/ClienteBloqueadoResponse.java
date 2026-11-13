package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDateTime;

public record ClienteBloqueadoResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        LocalDateTime bloqueadoAte
) {}
