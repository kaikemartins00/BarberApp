package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalDateTime;

public record ConversaResponse(
        Long id,
        String interlocutorNome,
        String ultimaMensagem,
        LocalDateTime ultimaMensagemEm,
        long naoLidas,
        boolean encerrada
) {}