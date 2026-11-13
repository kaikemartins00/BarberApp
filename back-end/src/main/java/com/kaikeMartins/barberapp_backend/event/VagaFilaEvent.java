package com.kaikeMartins.barberapp_backend.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record VagaFilaEvent(
        String email,
        String clienteNome,
        String barbeiroNome,
        String servicoNome,
        LocalDate dataAnterior,
        LocalTime horarioAnterior,
        LocalDate novaData,
        LocalTime novoHorario
) {}
