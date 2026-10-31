package com.kaikeMartins.barberapp_backend.dto;

import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;

import java.time.LocalDateTime;
import java.util.List;

public record ChamadoResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDateTime chamadoDate,
        ChamadoStatus status,
        PrioridadeChamado prioridade,
        String clienteNome,
        String clienteEmail,
        String responsavelNome,
        boolean podeAtender,
        List<RespostaChamResponse> respostas
) {}