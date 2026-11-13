package com.kaikeMartins.barberapp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReembolsoPendenteJob {

    private final PagamentoService pagamentoService;

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void reprocessar() {
        pagamentoService.reprocessarReembolsosPendentes();
    }
}
