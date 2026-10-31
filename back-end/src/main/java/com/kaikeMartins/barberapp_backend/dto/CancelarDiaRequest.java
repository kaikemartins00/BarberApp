package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CancelarDiaRequest(
        @NotNull @FutureOrPresent LocalDate data,
        LocalDate ateData,
        String motivo
) {}