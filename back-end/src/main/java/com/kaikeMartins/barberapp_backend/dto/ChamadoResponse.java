package com.kaikeMartins.barberapp_backend.dto;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;

import java.time.LocalDateTime;

public record ChamadoResponse (
        Long id,
        String titulo,
        String descricao,
        LocalDateTime chamadoDate,
        ChamadoStatus status,
        PrioridadeChamado prioridade,
        ClienteEntity cliente,
        BarbeiroEntity barbeiro

        )
{}
