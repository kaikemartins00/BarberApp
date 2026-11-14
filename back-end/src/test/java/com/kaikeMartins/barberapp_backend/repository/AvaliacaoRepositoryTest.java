package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.AvaliacaoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.support.CenarioPersistido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AvaliacaoRepositoryTest {

    @Autowired
    private AvaliacaoRepository repository;

    @Autowired
    private TestEntityManager em;

    private ClienteEntity cliente;
    private BarbeiroEntity barbeiro;
    private BarbeiroEntity outroBarbeiro;
    private AgendamentoEntity primeiro;
    private AgendamentoEntity segundo;
    private AgendamentoEntity doOutroBarbeiro;

    @BeforeEach
    void setUp() {
        CenarioPersistido cenario = new CenarioPersistido(em);
        cliente = cenario.cliente("cliente@email.com");
        barbeiro = cenario.barbeiro("barbeiro@email.com");
        outroBarbeiro = cenario.barbeiro("outro@email.com");
        var servico = cenario.servico("corte", 40.0);
        LocalDate dia = LocalDate.of(2026, 10, 1);
        primeiro = cenario.agendamento(cliente, barbeiro, servico, dia, LocalTime.of(9, 0), AgendamentoStatus.CONCLUIDO);
        segundo = cenario.agendamento(cliente, barbeiro, servico, dia, LocalTime.of(10, 0), AgendamentoStatus.CONCLUIDO);
        doOutroBarbeiro = cenario.agendamento(cliente, outroBarbeiro, servico, dia, LocalTime.of(11, 0), AgendamentoStatus.CONCLUIDO);
    }

    private AvaliacaoEntity avaliacao(AgendamentoEntity agendamento, int nota) {
        return em.persist(AvaliacaoEntity.builder()
                .agendamento(agendamento)
                .nota(nota)
                .comentario("ok")
                .tags("pontual")
                .criadaEm(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("Deve buscar a avaliação pelo agendamento")
    void findByAgendamentoId() {
        AvaliacaoEntity salva = avaliacao(primeiro, 5);

        assertEquals(salva.getId(), repository.findByAgendamentoId(primeiro.getId()).orElseThrow().getId());
        assertTrue(repository.findByAgendamentoId(segundo.getId()).isEmpty());
    }

    @Test
    @DisplayName("Deve informar se o agendamento já foi avaliado")
    void existsByAgendamentoId() {
        avaliacao(primeiro, 4);

        assertTrue(repository.existsByAgendamentoId(primeiro.getId()));
        assertFalse(repository.existsByAgendamentoId(segundo.getId()));
    }

    @Test
    @DisplayName("Deve listar apenas as avaliações do barbeiro")
    void findByAgendamentoBarbeiroId() {
        avaliacao(primeiro, 5);
        avaliacao(segundo, 3);
        avaliacao(doOutroBarbeiro, 1);

        List<AvaliacaoEntity> lista = repository.findByAgendamentoBarbeiroId(barbeiro.getId());

        assertEquals(2, lista.size());
        assertEquals(8, lista.stream().mapToInt(AvaliacaoEntity::getNota).sum());
    }

    @Test
    @DisplayName("Deve listar as avaliações feitas pelo cliente")
    void findByAgendamentoClienteId() {
        avaliacao(primeiro, 5);
        avaliacao(doOutroBarbeiro, 2);

        assertEquals(2, repository.findByAgendamentoClienteId(cliente.getId()).size());
    }
}
