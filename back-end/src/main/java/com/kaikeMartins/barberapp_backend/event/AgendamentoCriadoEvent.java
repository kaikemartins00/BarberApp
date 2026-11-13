package com.kaikeMartins.barberapp_backend.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoCriadoEvent(
        Long agendamentoId,
        String email,
        String clienteNome,
        String barbeiroNome,
        String servicoNome,
        Double preco,
        LocalDate data,
        LocalTime horario
) {}
