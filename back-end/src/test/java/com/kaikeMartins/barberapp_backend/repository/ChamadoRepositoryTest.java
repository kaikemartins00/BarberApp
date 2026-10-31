package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.ChamadoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChamadoRepositoryTest {

    @Autowired
    private ChamadoRepository repository;

    @Autowired
    private TestEntityManager em;

    private ClienteEntity kaike;
    private ClienteEntity maria;

    @BeforeEach
    void setUp() {
        UsuarioEntity usuarioKaike = em.persist(UsuarioEntity.builder()
                .email("kaike@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        UsuarioEntity usuarioMaria = em.persist(UsuarioEntity.builder()
                .email("maria@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        kaike = em.persist(ClienteEntity.builder()
                .usuario(usuarioKaike)
                .nome("Kaike")
                .telefone("79999999999")
                .build());

        maria = em.persist(ClienteEntity.builder()
                .usuario(usuarioMaria)
                .nome("Maria")
                .telefone("79977777777")
                .build());
    }

    private ChamadoEntity criarChamado(String titulo, LocalDateTime data, ClienteEntity cliente) {
        ChamadoEntity chamado = em.persist(ChamadoEntity.builder()
                .titulo(titulo)
                .descricao("descricao de " + titulo)
                .status(ChamadoStatus.ABERTO)
                .prioridade(PrioridadeChamado.MEDIA)
                .cliente(cliente)
                .build());

        em.flush();

        em.getEntityManager()
                .createNativeQuery("update chamados set chamado_date = :data where id = :id")
                .setParameter("data", data)
                .setParameter("id", chamado.getId())
                .executeUpdate();

        em.clear();

        return chamado;
    }

    @DisplayName("Salvar chamado preenche a data automaticamente")
    @Test
    void salvarChamado() {
        ChamadoEntity salvo = repository.save(ChamadoEntity.builder()
                .titulo("Sem agua quente")
                .descricao("O chuveiro nao esquenta")
                .status(ChamadoStatus.ABERTO)
                .prioridade(PrioridadeChamado.ALTA)
                .cliente(kaike)
                .build());

        em.flush();
        em.clear();

        ChamadoEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(doBanco.getChamadoDate());
        assertEquals("Sem agua quente", doBanco.getTitulo());
        assertEquals(ChamadoStatus.ABERTO, doBanco.getStatus());
        assertEquals(PrioridadeChamado.ALTA, doBanco.getPrioridade());
        assertEquals(kaike.getId(), doBanco.getCliente().getId());
    }

    @DisplayName("Lista todos do mais recente para o mais antigo")
    @Test
    void findAllByOrderByChamadoDateDesc() {
        criarChamado("antigo", LocalDateTime.of(2027, 1, 1, 10, 0), kaike);
        criarChamado("recente", LocalDateTime.of(2027, 3, 1, 10, 0), maria);
        criarChamado("meio", LocalDateTime.of(2027, 2, 1, 10, 0), kaike);

        List<ChamadoEntity> resultado = repository.findAllByOrderByChamadoDateDesc();
        List<String> titulos = resultado.stream().map(ChamadoEntity::getTitulo).toList();

        assertEquals(List.of("recente", "meio", "antigo"), titulos);
    }

    @DisplayName("Lista vazia quando não há chamados")
    @Test
    void findAllByOrderByChamadoDateDescVazio() {
        assertTrue(repository.findAllByOrderByChamadoDateDesc().isEmpty());
    }

    @DisplayName("Busca por cliente, do mais recente para o mais antigo")
    @Test
    void findByClienteIdOrderByChamadoDateDesc() {
        criarChamado("kaike antigo", LocalDateTime.of(2027, 1, 1, 10, 0), kaike);
        criarChamado("maria", LocalDateTime.of(2027, 2, 15, 10, 0), maria);
        criarChamado("kaike recente", LocalDateTime.of(2027, 3, 1, 10, 0), kaike);

        List<ChamadoEntity> resultado =
                repository.findByClienteIdOrderByChamadoDateDesc(kaike.getId());
        List<String> titulos = resultado.stream().map(ChamadoEntity::getTitulo).toList();

        assertEquals(List.of("kaike recente", "kaike antigo"), titulos);
        resultado.forEach(c -> assertEquals(kaike.getId(), c.getCliente().getId()));
    }

    @DisplayName("Cliente sem chamados devolve lista vazia")
    @Test
    void findByClienteIdSemChamados() {
        criarChamado("da maria", LocalDateTime.of(2027, 2, 15, 10, 0), maria);

        assertTrue(repository.findByClienteIdOrderByChamadoDateDesc(kaike.getId()).isEmpty());
    }

    @DisplayName("Busca chamado para atualizar com lock")
    @Test
    void buscarParaAtualizar() {
        ChamadoEntity chamado = criarChamado("para atualizar", LocalDateTime.of(2027, 2, 1, 10, 0), kaike);

        Optional<ChamadoEntity> resultado = repository.buscarParaAtualizar(chamado.getId());

        assertTrue(resultado.isPresent());
        assertEquals(chamado.getId(), resultado.get().getId());
        assertEquals("para atualizar", resultado.get().getTitulo());
    }

    @DisplayName("Busca para atualizar com id inexistente")
    @Test
    void buscarParaAtualizarInexistente() {
        assertTrue(repository.buscarParaAtualizar(9999L).isEmpty());
    }
}