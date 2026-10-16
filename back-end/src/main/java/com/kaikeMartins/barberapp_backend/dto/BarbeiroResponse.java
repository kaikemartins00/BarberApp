package com.kaikeMartins.barberapp_backend.dto;

import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;

import java.time.LocalTime;
import java.util.Set;

public record BarbeiroResponse(
        Long id,
        String nome,
        String fotoUrl,
        Set<ServicoEntity> servicos,
        Set<LocalTime> horariosTrabalho
) {}
