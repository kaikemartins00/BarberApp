package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.support.CenarioPersistido;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PagamentoRepositoryTest {

    @Autowired
    private PagamentoRepository repository;

    @Autowired
    private TestEntityManager em;

    private CenarioPersistido cenario;
    private AgendamentoEntity primeiro;
    private AgendamentoEntity segundo;

    @BeforeEach
    void setUp() {
        cenario = new CenarioPersistido(em);
        var cliente = cenario.cliente("cliente@email.com");
        var barbeiro = cenario.barbeiro("barbeiro@email.com");
        var servico = cenario.servico("corte", 40.0);
        primeiro = cenario.agendamento(cliente, barbeiro, servico, LocalDate.of(2027, 2, 12),
                LocalTime.of(14, 0), AgendamentoStatus.CONFIRMADO);
        segundo = cenario.agendamento(cliente, barbeiro, servico, LocalDate.of(2027, 2, 13),
                LocalTime.of(15, 0), AgendamentoStatus.PENDENTE);
    }

    private PagamentoEntity pagamento(AgendamentoEntity agendamento, PagamentoMetodo metodo,
                                      PagamentoStatus status, String mercadoPagoId) {
        return em.persist(PagamentoEntity.builder()
                .agendamento(agendamento)
                .valor(new BigDecimal("40.00"))
                .metodo(metodo)
                .status(status)
                .mercadoPagoId(mercadoPagoId)
                .build());
    }

    @Test
    @DisplayName("Deve buscar o pagamento pelo id do agendamento")
    void findByAgendamentoId() {
        PagamentoEntity salvo = pagamento(primeiro, PagamentoMetodo.PIX, PagamentoStatus.PAGO, "mp-1");

        assertEquals(salvo.getId(), repository.findByAgendamentoId(primeiro.getId()).orElseThrow().getId());
        assertTrue(repository.findByAgendamentoId(segundo.getId()).isEmpty());
    }

    @Test
    @DisplayName("Deve buscar o pagamento pelo id do Mercado Pago")
    void findByMercadoPagoId() {
        pagamento(primeiro, PagamentoMetodo.PIX, PagamentoStatus.PAGO, "mp-1");

        assertTrue(repository.findByMercadoPagoId("mp-1").isPresent());
        assertTrue(repository.findByMercadoPagoId("mp-inexistente").isEmpty());
    }

    @Test
    @DisplayName("Deve informar se o agendamento já possui pagamento")
    void existsByAgendamentoId() {
        pagamento(primeiro, PagamentoMetodo.PRESENCIAL, PagamentoStatus.PENDENTE, null);

        assertTrue(repository.existsByAgendamentoId(primeiro.getId()));
        assertFalse(repository.existsByAgendamentoId(segundo.getId()));
    }

    @Test
    @DisplayName("Deve listar apenas os pagamentos com reembolso pendente")
    void findByReembolsoPendenteTrue() {
        PagamentoEntity pendente = pagamento(primeiro, PagamentoMetodo.PIX, PagamentoStatus.PAGO, "mp-1");
        pendente.setReembolsoPendente(true);
        PagamentoEntity normal = pagamento(segundo, PagamentoMetodo.PIX, PagamentoStatus.PAGO, "mp-2");
        normal.setReembolsoPendente(false);
        em.flush();

        List<PagamentoEntity> lista = repository.findByReembolsoPendenteTrue();

        assertEquals(1, lista.size());
        assertEquals("mp-1", lista.get(0).getMercadoPagoId());
    }

    @Test
    @DisplayName("Deve filtrar pagamentos por status e método")
    void findByStatusAndMetodo() {
        pagamento(primeiro, PagamentoMetodo.PIX, PagamentoStatus.AGUARDANDO_CONFIRMACAO, "mp-1");
        pagamento(segundo, PagamentoMetodo.PRESENCIAL, PagamentoStatus.AGUARDANDO_CONFIRMACAO, null);

        List<PagamentoEntity> lista = repository
                .findByStatusAndMetodo(PagamentoStatus.AGUARDANDO_CONFIRMACAO, PagamentoMetodo.PIX);

        assertEquals(1, lista.size());
        assertEquals("mp-1", lista.get(0).getMercadoPagoId());
    }

    @Test
    @DisplayName("Deve declarar bloqueio pessimista de escrita na busca para atualização")
    void buscarParaAtualizacaoBloqueiaParaEscrita() throws NoSuchMethodException {
        Lock lock = PagamentoRepository.class.getMethod("buscarParaAtualizacao", Long.class).getAnnotation(Lock.class);

        assertNotNull(lock);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }
}
