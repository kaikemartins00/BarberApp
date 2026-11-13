package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDate;

public record OcupacaoResponse(
        Long barbeiroId,
        LocalDate data,
        int horariosDisponibilizados,
        int horariosOcupados,
        int horariosLivres,
        double percentualOcupacao
) {}
