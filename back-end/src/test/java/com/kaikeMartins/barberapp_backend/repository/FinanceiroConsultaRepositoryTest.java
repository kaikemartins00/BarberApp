package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.support.CenarioPersistido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class FinanceiroConsultaRepositoryTest {

    @Autowired
    private FinanceiroConsultaRepository repository;

    @Autowired
    private TestEntityManager em;

    private CenarioPersistido cenario;
    private BarbeiroEntity joao;
    private BarbeiroEntity pedro;
    private AgendamentoEntity doJoao;
    private AgendamentoEntity doPedro;
    private AgendamentoEntity canceladoDoJoao;

    @BeforeEach
    void setUp() {
        cenario = new CenarioPersistido(em);
        var cliente = cenario.cliente("cliente@email.com");
        joao = cenario.barbeiro("joao@email.com");
        pedro = cenario.barbeiro("pedro@email.com");
        var servico = cenario.servico("corte", 40.0);
        LocalDate dia = LocalDate.of(2027, 2, 12);
        doJoao = cenario.agendamento(cliente, joao, servico, dia, LocalTime.of(9, 0), AgendamentoStatus.CONCLUIDO);
        doPedro = cenario.agendamento(cliente, pedro, servico, dia, LocalTime.of(10, 0), AgendamentoStatus.CONCLUIDO);
        canceladoDoJoao = cenario.agendamento(cliente, joao, servico, dia, LocalTime.of(11, 0), AgendamentoStatus.CANCELADO);
    }

    private PagamentoEntity pagamento(AgendamentoEntity agendamento, PagamentoStatus status) {
        return em.persist(PagamentoEntity.builder()
                .agendamento(agendamento)
                .valor(new BigDecimal("40.00"))
                .metodo(PagamentoMetodo.PRESENCIAL)
                .status(status)
                .build());
    }

    @Test
    @DisplayName("Deve listar pagamentos pagos ignorando agendamentos cancelados")
    void findByStatusAndAgendamentoStatusNot() {
        PagamentoEntity pagoJoao = pagamento(doJoao, PagamentoStatus.PAGO);
        PagamentoEntity pagoPedro = pagamento(doPedro, PagamentoStatus.PAGO);
        pagamento(canceladoDoJoao, PagamentoStatus.PAGO);

        List<PagamentoEntity> lista = repository
                .findByStatusAndAgendamentoStatusNot(PagamentoStatus.PAGO, AgendamentoStatus.CANCELADO);

        assertEquals(2, lista.size());
        assertTrue(lista.stream().map(PagamentoEntity::getId)
                .allMatch(id -> id.equals(pagoJoao.getId()) || id.equals(pagoPedro.getId())));
    }

    @Test
    @DisplayName("Deve ignorar pagamentos que ainda não foram pagos")
    void ignoraPagamentosPendentes() {
        pagamento(doJoao, PagamentoStatus.PENDENTE);

        assertTrue(repository.findByStatusAndAgendamentoStatusNot(PagamentoStatus.PAGO, AgendamentoStatus.CANCELADO).isEmpty());
    }

    @Test
    @DisplayName("Deve listar os pagamentos pagos de um único barbeiro")
    void findPorBarbeiro() {
        PagamentoEntity pagoJoao = pagamento(doJoao, PagamentoStatus.PAGO);
        pagamento(doPedro, PagamentoStatus.PAGO);
        pagamento(canceladoDoJoao, PagamentoStatus.PAGO);

        List<PagamentoEntity> lista = repository.findByStatusAndAgendamentoStatusNotAndAgendamentoBarbeiroId(
                PagamentoStatus.PAGO, AgendamentoStatus.CANCELADO, joao.getId());

        assertEquals(1, lista.size());
        assertEquals(pagoJoao.getId(), lista.get(0).getId());
    }
}
