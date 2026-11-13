package com.kaikeMartins.barberapp_backend.dto;

import java.math.BigDecimal;

public record ServicoAtendimentoResponse(
        Long id,
        Long servicoId,
        String servicoNome,
        BigDecimal valorCobrado,
        boolean extra
) {}
