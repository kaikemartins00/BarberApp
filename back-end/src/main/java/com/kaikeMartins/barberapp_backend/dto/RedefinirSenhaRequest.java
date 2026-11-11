package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank @Email String email,
        @NotBlank String codigo,
        @NotBlank @Size(min = 6, max = 72) String novaSenha
) {}