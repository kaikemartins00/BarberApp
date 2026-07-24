package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ServicoRequest(

        @NotBlank
        String nome,

        @NotNull
        Double preco

)
{}
