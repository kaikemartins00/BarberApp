package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record FilaEsperaRequest(
        @NotNull Long agendamentoAlternativoId,
        @NotNull @FutureOrPresent LocalDate dataPreferida,
        @NotEmpty Set<@NotNull LocalTime> horariosAceitos
) {}
