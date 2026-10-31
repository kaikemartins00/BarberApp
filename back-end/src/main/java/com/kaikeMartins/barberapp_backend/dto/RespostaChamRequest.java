package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RespostaChamRequest (@NotBlank String respostaMsg) {}
