package com.kaikeMartins.barberapp_backend.dto;

import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record BarbeiroResponse(
        Long id,
        String nome,
        String fotoUrl,
        Set<ServicoEntity> servicos,
        List<LocalDateTime> horarios
) {}