package com.kaikeMartins.barberapp_backend.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record PagamentoAprovadoEvent(
        Long agendamentoId,
        String email,
        String clienteNome,
        String barbeiroNome,
        String servicoNome,
        LocalDate data,
        LocalTime horario,
        BigDecimal valor
) {}
