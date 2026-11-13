package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record AvaliacaoRequest(
        @NotNull Long agendamentoId,
        @Min(1) @Max(5) int nota,
        @Size(max=500) String comentario,
        List<@Size(max=40) String> caracteristicas
) {}
