package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoRequest(
        @NotNull Long barbeiroId,
        @NotNull Long servicoId,
        @NotNull @Future LocalDate data,
        @NotNull LocalTime horario
)
{}