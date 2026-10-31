package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensagemRequest(@NotBlank @Size(max = 1000) String texto) {}
