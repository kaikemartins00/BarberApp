package com.kaikeMartins.barberapp_backend.dto;

import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;

import java.time.LocalDateTime;


public record HistoricoResponse(

        Long id,

        String role,

        String msg,

        LocalDateTime msgDate,

        ClienteEntity cliente
) {
}
