package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record HorariosRequest(

        @NotNull List<LocalDateTime> horarios
) {}