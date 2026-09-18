package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ChamadoRequest (
    @NotBlank
    String titulo,
    @NotBlank
    String descricao
)
{}
