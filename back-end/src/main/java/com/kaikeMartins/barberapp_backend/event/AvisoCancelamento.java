package com.kaikeMartins.barberapp_backend.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvisoCancelamento(
        String email,
        String clienteNome,
        String barbeiroNome,
        String servicoNome,
        LocalDate data,
        LocalTime horario,
        String motivo
) {}