package com.kaikeMartins.barberapp_backend.dto;

import java.math.BigDecimal;

public record FinanceiroResumoResponse(
        BigDecimal totalBruto,
        BigDecimal taxaLoja,
        BigDecimal totalLiquido,
        long quantidadePagamentos
) {}
