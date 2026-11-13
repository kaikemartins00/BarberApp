package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.Size;

public record PreferenciasClienteRequest(
        @Size(max = 2000, message = "As preferências podem ter no máximo 2000 caracteres.")
        String observacoes
) {}
