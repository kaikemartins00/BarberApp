package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AgendamentoServicoRepositoryTest {

    @Autowired
    private AgendamentoServicoRepository repository;

    @Autowired
    private TestEntityManager em;

    private AgendamentoEntity primeiro;
    private AgendamentoEntity segundo;
    private ServicoEntity corte;
    private ServicoEntity barba;

    @BeforeEach
    void setUp() {
        CenarioPersistido cenario = new CenarioPersistido(em);
        var cliente = cenario.cliente("cliente@email.com");
        var barbeiro = cenario.barbeiro("barbeiro@email.com");
        corte = cenario.servico("corte", 40.0);
        barba = cenario.servico("barba", 25.0);
        primeiro = cenario.agendamento(cliente, barbeiro, corte, LocalDate.of(2027, 2, 12), LocalTime.of(14, 0), AgendamentoStatus.CONFIRMADO);
        segundo = cenario.agendamento(cliente, barbeiro, corte, LocalDate.of(2027, 2, 13), LocalTime.of(15, 0), AgendamentoStatus.CONFIRMADO);
    }

    private AgendamentoServicoEntity item(AgendamentoEntity agendamento, ServicoEntity servico, String valor, boolean extra) {
        return em.persist(AgendamentoServicoEntity.builder()
                .agendamento(agendamento)
                .servico(servico)
                .valorCobrado(new BigDecimal(valor))
                .extra(extra)
                .build());
    }

    @Test
    @DisplayName("Deve listar os itens do agendamento em ordem de criação")
    void findByAgendamentoIdOrderByIdAsc() {
        AgendamentoServicoEntity original = item(primeiro, corte, "40.00", false);
        AgendamentoServicoEntity extra = item(primeiro, barba, "25.00", true);
        item(segundo, corte, "40.00", false);

        List<AgendamentoServicoEntity> itens = repository.findByAgendamentoIdOrderByIdAsc(primeiro.getId());

        assertEquals(List.of(original.getId(), extra.getId()), itens.stream().map(AgendamentoServicoEntity::getId).toList());
    }

    @Test
    @DisplayName("Deve buscar o item apenas dentro do agendamento informado")
    void findByIdAndAgendamentoId() {
        AgendamentoServicoEntity salvo = item(primeiro, corte, "40.00", false);

        assertTrue(repository.findByIdAndAgendamentoId(salvo.getId(), primeiro.getId()).isPresent());
        assertTrue(repository.findByIdAndAgendamentoId(salvo.getId(), segundo.getId()).isEmpty());
    }

    @Test
    @DisplayName("Deve persistir o valor cobrado com duas casas e a marca de extra")
    void persisteValorEExtra() {
        AgendamentoServicoEntity salvo = item(primeiro, barba, "25.50", true);
        em.flush();
        em.clear();

        AgendamentoServicoEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertEquals(new BigDecimal("25.50"), doBanco.getValorCobrado());
        assertTrue(doBanco.isExtra());
    }
}
