package com.kaikeMartins.barberapp_backend.dto;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ChamadoEntity;

import java.time.LocalDateTime;

public record RespostaChamResponse (
        Long id,
        String respostaMsg,
        LocalDateTime dataEnvio,
        BarbeiroEntity barbeiro,
        ChamadoEntity chamado
)
{}
