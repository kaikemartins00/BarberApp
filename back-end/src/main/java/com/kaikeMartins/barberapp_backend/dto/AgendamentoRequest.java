package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoRequest(
        @NotNull Long barbeiroId,
        @NotNull Long servicoId,
        @NotNull @FutureOrPresent LocalDate data,
        @NotNull LocalTime horario
)
{}