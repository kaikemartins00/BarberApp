package com.kaikeMartins.barberapp_backend.dto;

import java.math.BigDecimal;

public record PixResponse(
        Long agendamentoId,
        BigDecimal valorTotal,
        String qrCodeBase64,
        String pixCopiaECola
) {}
