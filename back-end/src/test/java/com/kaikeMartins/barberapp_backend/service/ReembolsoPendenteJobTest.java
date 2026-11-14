package com.kaikeMartins.barberapp_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReembolsoPendenteJobTest {

    @InjectMocks
    private ReembolsoPendenteJob job;

    @Mock
    private PagamentoService pagamentoService;

    @Test
    @DisplayName("Deve reprocessar os reembolsos pendentes a cada execução")
    void reprocessar() {
        job.reprocessar();

        verify(pagamentoService).reprocessarReembolsosPendentes();
    }
}
