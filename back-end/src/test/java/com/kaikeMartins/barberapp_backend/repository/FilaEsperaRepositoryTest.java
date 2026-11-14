package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.FilaEsperaEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.FilaEsperaStatus;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FilaEsperaRepositoryTest {

    private static final LocalDate DIA_DESEJADO = LocalDate.of(2027, 2, 10);

    @Autowired
    private FilaEsperaRepository repository;

    @Autowired
    private TestEntityManager em;

    private ClienteEntity clienteA;
    private ClienteEntity clienteB;
    private BarbeiroEntity barbeiro;
    private BarbeiroEntity outroBarbeiro;
    private AgendamentoEntity alternativoA;
    private AgendamentoEntity alternativoB;
    private AgendamentoEntity alternativoOutroBarbeiro;

    @BeforeEach
    void setUp() {
        CenarioPersistido cenario = new CenarioPersistido(em);
        clienteA = cenario.cliente("a@email.com");
        clienteB = cenario.cliente("b@email.com");
        barbeiro = cenario.barbeiro("barbeiro@email.com");
        outroBarbeiro = cenario.barbeiro("outro@email.com");
        ServicoEntity servico = cenario.servico("corte", 40.0);
        LocalDate dia = LocalDate.of(2027, 2, 15);
        alternativoA = cenario.agendamento(clienteA, barbeiro, servico, dia, LocalTime.of(14, 0), AgendamentoStatus.CONFIRMADO);
        alternativoB = cenario.agendamento(clienteB, barbeiro, servico, dia, LocalTime.of(15, 0), AgendamentoStatus.CONFIRMADO);
        alternativoOutroBarbeiro = cenario.agendamento(clienteA, outroBarbeiro, servico, dia, LocalTime.of(16, 0), AgendamentoStatus.CONFIRMADO);
    }

    private FilaEsperaEntity inscricao(AgendamentoEntity agendamento, LocalDate dataPreferida,
                                       FilaEsperaStatus status, LocalDateTime criadoEm) {
        return em.persist(FilaEsperaEntity.builder()
                .agendamento(agendamento)
                .dataPreferida(dataPreferida)
                .horariosAceitos(new HashSet<>(Set.of(LocalTime.of(10, 0))))
                .status(status)
                .criadoEm(criadoEm)
                .build());
    }

    @Test
    @DisplayName("Deve persistir os horários aceitos da inscrição")
    void persisteHorariosAceitos() {
        FilaEsperaEntity salva = inscricao(alternativoA, DIA_DESEJADO, FilaEsperaStatus.AGUARDANDO, LocalDateTime.now());
        em.flush();
        em.clear();

        FilaEsperaEntity doBanco = repository.findById(salva.getId()).orElseThrow();

        assertEquals(Set.of(LocalTime.of(10, 0)), doBanco.getHorariosAceitos());
    }

    @Test
    @DisplayName("Deve ordenar a fila do barbeiro por ordem de chegada e ignorar outros dias, status e barbeiros")
    void buscarFilaOrdenada() {
        LocalDateTime agora = LocalDateTime.now();
        FilaEsperaEntity segunda = inscricao(alternativoB, DIA_DESEJADO, FilaEsperaStatus.AGUARDANDO, agora.minusMinutes(5));
        FilaEsperaEntity primeira = inscricao(alternativoA, DIA_DESEJADO, FilaEsperaStatus.AGUARDANDO, agora.minusMinutes(30));
        inscricao(alternativoA, DIA_DESEJADO.plusDays(1), FilaEsperaStatus.AGUARDANDO, agora);
        inscricao(alternativoB, DIA_DESEJADO, FilaEsperaStatus.CANCELADO, agora);
        inscricao(alternativoOutroBarbeiro, DIA_DESEJADO, FilaEsperaStatus.AGUARDANDO, agora);

        List<FilaEsperaEntity> fila = repository.buscarFila(barbeiro.getId(), DIA_DESEJADO, FilaEsperaStatus.AGUARDANDO);

        assertEquals(List.of(primeira.getId(), segunda.getId()), fila.stream().map(FilaEsperaEntity::getId).toList());
    }

    @Test
    @DisplayName("Deve informar se o agendamento já tem inscrição com o status")
    void existsByAgendamentoIdAndStatus() {
        inscricao(alternativoA, DIA_DESEJADO, FilaEsperaStatus.AGUARDANDO, LocalDateTime.now());

        assertTrue(repository.existsByAgendamentoIdAndStatus(alternativoA.getId(), FilaEsperaStatus.AGUARDANDO));
        assertFalse(repository.existsByAgendamentoIdAndStatus(alternativoA.getId(), FilaEsperaStatus.CANCELADO));
        assertFalse(repository.existsByAgendamentoIdAndStatus(alternativoB.getId(), FilaEsperaStatus.AGUARDANDO));
    }

    @Test
    @DisplayName("Deve buscar a inscrição apenas para o cliente dono")
    void findByIdAndAgendamentoClienteId() {
        FilaEsperaEntity salva = inscricao(alternativoA, DIA_DESEJADO, FilaEsperaStatus.AGUARDANDO, LocalDateTime.now());

        assertTrue(repository.findByIdAndAgendamentoClienteId(salva.getId(), clienteA.getId()).isPresent());
        assertTrue(repository.findByIdAndAgendamentoClienteId(salva.getId(), clienteB.getId()).isEmpty());
    }

    @Test
    @DisplayName("Deve listar as inscrições de um agendamento por status")
    void findByAgendamentoIdAndStatus() {
        inscricao(alternativoA, DIA_DESEJADO, FilaEsperaStatus.AGUARDANDO, LocalDateTime.now());
        inscricao(alternativoA, DIA_DESEJADO.plusDays(1), FilaEsperaStatus.ATENDIDO, LocalDateTime.now());

        List<FilaEsperaEntity> aguardando = repository
                .findByAgendamentoIdAndStatus(alternativoA.getId(), FilaEsperaStatus.AGUARDANDO);

        assertEquals(1, aguardando.size());
        assertEquals(DIA_DESEJADO, aguardando.get(0).getDataPreferida());
    }

    @Test
    @DisplayName("Deve listar inscrições aguardando cuja data preferida já passou")
    void findByStatusAndDataPreferidaBefore() {
        inscricao(alternativoA, DIA_DESEJADO.minusDays(3), FilaEsperaStatus.AGUARDANDO, LocalDateTime.now());
        inscricao(alternativoB, DIA_DESEJADO.plusDays(3), FilaEsperaStatus.AGUARDANDO, LocalDateTime.now());

        List<FilaEsperaEntity> vencidas = repository
                .findByStatusAndDataPreferidaBefore(FilaEsperaStatus.AGUARDANDO, DIA_DESEJADO);

        assertEquals(1, vencidas.size());
        assertEquals(DIA_DESEJADO.minusDays(3), vencidas.get(0).getDataPreferida());
    }
}
