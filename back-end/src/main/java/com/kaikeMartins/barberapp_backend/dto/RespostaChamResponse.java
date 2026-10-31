package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDateTime;

public record RespostaChamResponse (Long id, String respostaMsg, LocalDateTime dataEnvio, String autorNome) {}
