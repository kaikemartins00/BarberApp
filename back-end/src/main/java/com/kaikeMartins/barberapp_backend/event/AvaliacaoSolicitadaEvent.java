package com.kaikeMartins.barberapp_backend.event;

public record AvaliacaoSolicitadaEvent(
        Long agendamentoId,
        String emailCliente,
        String nomeCliente
) {}
