package com.kaikeMartins.barberapp_backend.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ComprovanteCancelamentoEvent(
        Long agendamentoId,
        String email,
        String clienteNome,
        String servicoNome,
        String barbeiroNome,
        LocalDate data,
        LocalTime horario,
        boolean pelaBarbearia,
        boolean comAntecedencia,
        int cancelamentosUsados,
        int limite,
        LocalDateTime bloqueadoAte,
        int diasBloqueio
) {}
