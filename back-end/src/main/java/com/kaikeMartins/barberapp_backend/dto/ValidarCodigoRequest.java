package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ValidarCodigoRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        String codigo
)
{}