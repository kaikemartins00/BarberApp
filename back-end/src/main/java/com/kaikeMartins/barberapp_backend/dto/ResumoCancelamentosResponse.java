package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDateTime;

public record ResumoCancelamentosResponse (
        int usados,
        int limite,
        int diasBloqueio,
        LocalDateTime bloqueadoAte
) {}
