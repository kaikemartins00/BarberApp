package com.kaikeMartins.barberapp_backend.event;

import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoNotificacaoTipo;

public record ChamadoNotificacaoEvent(
        ChamadoNotificacaoTipo tipo,
        Long chamadoId,
        String email,
        String clienteNome,
        String titulo,
        String texto,
        String atendente
) {
}