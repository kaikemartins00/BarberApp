package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record BarbeiroRequest (

        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank String senha,
        String fotoUrl
)
{}

