package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DesligarBarbeiroRequest(
        @NotBlank @Size(min = 5, max = 500) String motivo
) {}
