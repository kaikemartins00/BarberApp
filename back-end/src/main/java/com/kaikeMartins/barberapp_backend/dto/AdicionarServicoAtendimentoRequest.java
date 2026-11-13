package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdicionarServicoAtendimentoRequest(
        @NotNull Long servicoId,
        @DecimalMin("0.01") BigDecimal valorCobrado
) {}
