package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PixConciliacaoJobTest {

    @InjectMocks
    private PixConciliacaoJob job;

    @Mock
    private PagamentoRepository repository;

    @Mock
    private PixService pixService;

    private PagamentoEntity pix(String mercadoPagoId) {
        return PagamentoEntity.builder().mercadoPagoId(mercadoPagoId).build();
    }

    @Test
    @DisplayName("Deve sincronizar cada Pix aguardando confirmação")
    void conciliar() {
        when(repository.findByStatusAndMetodo(PagamentoStatus.AGUARDANDO_CONFIRMACAO, PagamentoMetodo.PIX))
                .thenReturn(List.of(pix("mp-1"), pix("mp-2")));

        job.conciliar();

        verify(pixService).sincronizar("mp-1");
        verify(pixService).sincronizar("mp-2");
    }

    @Test
    @DisplayName("Deve ignorar pagamentos sem id do Mercado Pago")
    void conciliarIgnoraSemId() {
        when(repository.findByStatusAndMetodo(PagamentoStatus.AGUARDANDO_CONFIRMACAO, PagamentoMetodo.PIX))
                .thenReturn(List.of(pix(null), pix("mp-1")));

        job.conciliar();

        verify(pixService).sincronizar("mp-1");
        verify(pixService, never()).sincronizar(null);
    }

    @Test
    @DisplayName("Deve continuar sincronizando quando um Pix falha")
    void conciliarContinuaAposFalha() {
        when(repository.findByStatusAndMetodo(PagamentoStatus.AGUARDANDO_CONFIRMACAO, PagamentoMetodo.PIX))
                .thenReturn(List.of(pix("mp-1"), pix("mp-2")));
        doThrow(new IllegalStateException("indisponível")).when(pixService).sincronizar("mp-1");

        job.conciliar();

        verify(pixService).sincronizar("mp-2");
    }
}
