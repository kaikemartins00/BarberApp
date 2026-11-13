package com.kaikeMartins.barberapp_backend.dto;

import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull
        @Valid
        AgendamentoRequest agendamento,
        @NotNull
        PagamentoMetodo metodo
) {}
