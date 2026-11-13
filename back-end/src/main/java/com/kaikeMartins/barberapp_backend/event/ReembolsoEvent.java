package com.kaikeMartins.barberapp_backend.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReembolsoEvent(
        Long agendamentoId,
        String email,
        String clienteNome,
        String servicoNome,
        LocalDate data,
        LocalTime horario,
        BigDecimal valor,
        String codigoTransacao,
        LocalDateTime efetuadoEm
) {}
