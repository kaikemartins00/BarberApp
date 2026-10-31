package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ServicosIdsRequest(@NotNull List<Long> servicosIds) {}
