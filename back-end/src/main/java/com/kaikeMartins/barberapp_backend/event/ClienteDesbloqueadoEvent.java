package com.kaikeMartins.barberapp_backend.event;

public record ClienteDesbloqueadoEvent(
        String email,
        String clienteNome,
        int limite
) {}
