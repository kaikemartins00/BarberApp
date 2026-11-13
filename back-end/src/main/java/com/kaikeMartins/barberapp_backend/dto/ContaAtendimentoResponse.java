package com.kaikeMartins.barberapp_backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record ContaAtendimentoResponse(
        Long agendamentoId,
        List<ServicoAtendimentoResponse> servicos,
        BigDecimal valorTotal,
        String pagamentoStatus
) {}
