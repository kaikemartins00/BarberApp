package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoConfirmadoDto(
        String email,
        String clienteNome,
        String barbeiroNome,
        String servicoNome,
        LocalDate data,
        LocalTime horario
) {}
