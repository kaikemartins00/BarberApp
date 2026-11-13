package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AlterarValorServicoRequest(
        @NotNull
        @DecimalMin("0.01")
        BigDecimal valorCobrado
) {}
