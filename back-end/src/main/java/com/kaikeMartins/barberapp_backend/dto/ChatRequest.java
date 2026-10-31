package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 500)
        String mensagem
) {}
