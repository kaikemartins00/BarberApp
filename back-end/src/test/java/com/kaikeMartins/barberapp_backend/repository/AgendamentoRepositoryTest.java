package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AgendamentoRepositoryTest {

    @Autowired
    private AgendamentoRepository repository;

    @Autowired
    private TestEntityManager em;

    private AgendamentoEntity agendamento;
    private ClienteEntity cliente;
    private BarbeiroEntity barbeiro;
    private ServicoEntity servico;

    private UsuarioEntity usuarioCliente;
    private UsuarioEntity usuarioBarbeiro;


    @BeforeEach
    void setUp() {
        usuarioCliente = em.persist(UsuarioEntity.builder()
                .email("kaike@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        usuarioBarbeiro = em.persist(UsuarioEntity.builder()
                .email("joao@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        cliente = em.persist(ClienteEntity.builder()
                .usuario(usuarioCliente)
                .nome("Kaike")
                .telefone("79999999999")
                .build());

        barbeiro = em.persist(BarbeiroEntity.builder()
                .usuario(usuarioBarbeiro)
                .nome("Joao")
                .telefone("79988888888")
                .build());

        servico = em.persist(ServicoEntity.builder()
                .nome("corte")
                .preco(40.00)
                .build());

        agendamento = em.persist(AgendamentoEntity.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(LocalDate.of(2027, 2, 12))
                .horario(LocalTime.of(14, 0))
                .status(AgendamentoStatus.PENDENTE)
                .build());
    }

    @DisplayName("Salvar agendamento")
    @Test
    void salvarAgendamento() {
        AgendamentoEntity novo = AgendamentoEntity.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(LocalDate.of(2027, 2, 12))
                .horario(LocalTime.of(14, 0))
                .status(AgendamentoStatus.PENDENTE)
                .build();

        AgendamentoEntity salvo = repository.save(novo);

        AgendamentoEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertNotNull(doBanco.getCriadoEm());
        assertEquals(LocalDate.of(2027, 2, 12), doBanco.getData());
        assertEquals(LocalTime.of(14, 0), doBanco.getHorario());
        assertEquals(AgendamentoStatus.PENDENTE, doBanco.getStatus());
        assertEquals("Kaike", doBanco.getCliente().getNome());
        assertEquals("Joao", doBanco.getBarbeiro().getNome());
        assertEquals("corte", doBanco.getServico().getNome());
    }

    @DisplayName("Listar")
    @Test
    void listar() {
        AgendamentoEntity agendamento1 = AgendamentoEntity.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(LocalDate.of(2027, 2, 12))
                .horario(LocalTime.of(14, 0))
                .status(AgendamentoStatus.PENDENTE)
                .build();

        repository.save(agendamento);
        repository.save(agendamento1);

        List<AgendamentoEntity> agendamentoList = repository.findAll();

        assertNotNull(agendamentoList);
        assertEquals(2, agendamentoList.size());
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {

        repository.save(agendamento);

        AgendamentoEntity agendamentoSalvo = repository.findById(agendamento.getId()).get();

        assertNotNull(agendamentoSalvo);
        assertEquals(agendamento.getId(), agendamentoSalvo.getId());
    }

    @DisplayName("Busca Id do Cliente")
    @Test
    void findByClienteId() {

        repository.save(agendamento);

        List<AgendamentoEntity> resultado = repository.findByClienteId(cliente.getId());

        assertFalse(resultado.isEmpty());
        assertEquals(cliente.getId(), resultado.get(0).getCliente().getId());
    }

    @DisplayName("Busca por Id do Barbeiro")
    @Test
    void findByBarbeiroId() {
        AgendamentoEntity agendamento1 = AgendamentoEntity.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(LocalDate.of(2027, 2, 13))
                .horario(LocalTime.of(15, 0))
                .status(AgendamentoStatus.PENDENTE)
                .build();

        repository.save(agendamento1);

        List<AgendamentoEntity> resultado = repository.findByBarbeiroId(barbeiro.getId());

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(a -> a.getBarbeiro().getId().equals(barbeiro.getId())));
    }

    @DisplayName("Existe agendamento no mesmo horário com status diferente de CANCELADO")
    @Test
    void existsByBarbeiroIdAndDataAndHorarioAndStatusNot() {
        boolean existe = repository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                barbeiro.getId(), LocalDate.of(2027, 2, 12),
                LocalTime.of(14, 0), AgendamentoStatus.CANCELADO);

        assertTrue(existe);
    }

    @DisplayName("Não existe quando o único agendamento está cancelado")
    @Test
    void existsIgnoraCancelado() {
        agendamento.setStatus(AgendamentoStatus.CANCELADO);
        em.persistAndFlush(agendamento);

        boolean existe = repository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                barbeiro.getId(), LocalDate.of(2027, 2, 12),
                LocalTime.of(14, 0), AgendamentoStatus.CANCELADO);

        assertFalse(existe);
    }

    @DisplayName("existsByServicoId")
    @Test
    void existsByServicoId() {
        assertTrue(repository.existsByServicoId(servico.getId()));
        assertFalse(repository.existsByServicoId(999L));
    }

    @DisplayName("Busca por barbeiro e data")
    @Test
    void findByBarbeiroIdAndData() {
        List<AgendamentoEntity> resultado =
                repository.findByBarbeiroIdAndData(barbeiro.getId(), LocalDate.of(2027, 2, 12));
        assertEquals(1, resultado.size());

        assertTrue(repository.findByBarbeiroIdAndData(barbeiro.getId(), LocalDate.of(2027, 3, 1)).isEmpty());
    }

    @DisplayName("Busca por período e status")
    @Test
    void findByBarbeiroIdAndDataBetweenAndStatusIn() {
        List<AgendamentoEntity> resultado = repository.findByBarbeiroIdAndDataBetweenAndStatusIn(
                barbeiro.getId(),
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 28),
                List.of(AgendamentoStatus.PENDENTE));
        assertEquals(1, resultado.size());

        assertTrue(repository.findByBarbeiroIdAndDataBetweenAndStatusIn(
                barbeiro.getId(),
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 28),
                List.of(AgendamentoStatus.CANCELADO)).isEmpty());
    }

    @DisplayName("Deletar produto")
    @Test
    void deletar() {
        repository.deleteById(agendamento.getId());

        assertTrue(repository.findById(agendamento.getId()).isEmpty());
    }
}
