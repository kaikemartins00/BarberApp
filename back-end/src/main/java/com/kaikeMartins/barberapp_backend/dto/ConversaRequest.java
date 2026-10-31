package com.kaikeMartins.barberapp_backend.dto;

import jakarta.validation.constraints.NotNull;

public record ConversaRequest(@NotNull Long barbeiroId) {}
