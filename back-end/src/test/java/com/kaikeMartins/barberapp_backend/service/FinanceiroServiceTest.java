package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.FinanceiroResumoResponse;
import com.kaikeMartins.barberapp_backend.repository.FinanceiroConsultaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceiroServiceTest {

    @InjectMocks
    private FinanceiroService service;

    @Mock
    private FinanceiroConsultaRepository repository;

    @AfterEach
    void tearDown() {
        limparAutenticacao();
    }

    private PagamentoEntity pagamento(String valor) {
        return PagamentoEntity.builder().valor(new BigDecimal(valor)).build();
    }

    @Test
    @DisplayName("Deve calcular o resumo geral com 20% para a loja")
    void geral() {
        when(repository.findByStatusAndAgendamentoStatusNot(PagamentoStatus.PAGO, AgendamentoStatus.CANCELADO))
                .thenReturn(List.of(pagamento("40.00"), pagamento("60.00"), pagamento("25.50")));

        FinanceiroResumoResponse resumo = service.geral();

        assertEquals(new BigDecimal("125.50"), resumo.totalBruto());
        assertEquals(new BigDecimal("25.10"), resumo.taxaLoja());
        assertEquals(new BigDecimal("100.40"), resumo.totalLiquido());
        assertEquals(3, resumo.quantidadePagamentos());
    }

    @Test
    @DisplayName("Deve devolver tudo zerado quando não há pagamentos")
    void geralSemPagamentos() {
        when(repository.findByStatusAndAgendamentoStatusNot(PagamentoStatus.PAGO, AgendamentoStatus.CANCELADO))
                .thenReturn(List.of());

        FinanceiroResumoResponse resumo = service.geral();

        assertEquals(new BigDecimal("0.00"), resumo.totalBruto());
        assertEquals(new BigDecimal("0.00"), resumo.taxaLoja());
        assertEquals(new BigDecimal("0.00"), resumo.totalLiquido());
        assertEquals(0, resumo.quantidadePagamentos());
    }

    @Test
    @DisplayName("Deve calcular o resumo apenas do barbeiro logado")
    void meu() {
        autenticar(usuario(2L, BARBEIRO));
        when(repository.findByStatusAndAgendamentoStatusNotAndAgendamentoBarbeiroId(
                PagamentoStatus.PAGO, AgendamentoStatus.CANCELADO, 2L))
                .thenReturn(List.of(pagamento("100.00")));

        FinanceiroResumoResponse resumo = service.meu();

        assertEquals(new BigDecimal("100.00"), resumo.totalBruto());
        assertEquals(new BigDecimal("20.00"), resumo.taxaLoja());
        assertEquals(new BigDecimal("80.00"), resumo.totalLiquido());
        assertEquals(1, resumo.quantidadePagamentos());
    }
}
