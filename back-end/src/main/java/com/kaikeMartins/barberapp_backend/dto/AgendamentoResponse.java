package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoResponse (
        Long id,
        String clienteNome,
        String barbeiroNome,
        String servicoNome,
        Double servicoPreco,
        LocalDate data,
        LocalTime horario,
        String status
){}
