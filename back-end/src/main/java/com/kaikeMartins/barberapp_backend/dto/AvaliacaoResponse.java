package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AvaliacaoResponse(
        Long id, Long agendamentoId,
        Long barbeiroId, int nota,
        String comentario,
        List<String> tags,
        LocalDateTime criadaEm
) {}
