package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroRequest (

        @NotBlank
        String nome,
        @NotBlank
        @Email
        String email,
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "A senha deve conter pelo menos uma letra maiúscula, uma minúscula e um número."
        )
        String senha,
        // aceite dos Termos de Uso e da Política de Privacidade (guardamos a data e a versão)
        @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade.")
        Boolean aceitouTermos,
        String versaoTermos
){}
