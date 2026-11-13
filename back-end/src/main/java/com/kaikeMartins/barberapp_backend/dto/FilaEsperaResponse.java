package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record FilaEsperaResponse(
        Long id,
        Long agendamentoId,
        LocalDate dataPreferida,
        Set<LocalTime> horariosAceitos,
        LocalDate dataAgendada,
        LocalTime horarioAgendado,
        String status
) {}
