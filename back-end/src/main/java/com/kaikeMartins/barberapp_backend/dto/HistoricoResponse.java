package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDateTime;

public record HistoricoResponse(
        Long id,
        String role,
        String msg,
        LocalDateTime msgDate
) {}
