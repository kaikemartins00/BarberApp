package com.kaikeMartins.barberapp_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponse(
        Long id,
        Long agendamentoId,
        BigDecimal valor,
        String metodo,
        String status,
        LocalDateTime confirmadoEm
) {
}
