package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ConversaEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ConversaRepositoryTest {

    @Autowired
    private ConversaRepository repository;

    @Autowired
    private TestEntityManager em;

    private ClienteEntity kaike;
    private ClienteEntity maria;
    private BarbeiroEntity joao;
    private BarbeiroEntity pedro;

    private ConversaEntity kaikeJoao;
    private ConversaEntity kaikePedro;
    private ConversaEntity mariaJoao;

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

        UsuarioEntity usuarioJoao = em.persist(UsuarioEntity.builder()
                .email("joao@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        UsuarioEntity usuarioPedro = em.persist(UsuarioEntity.builder()
                .email("pedro@email.com")
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

        joao = em.persist(BarbeiroEntity.builder()
                .usuario(usuarioJoao)
                .nome("Joao")
                .telefone("79988888888")
                .build());

        pedro = em.persist(BarbeiroEntity.builder()
                .usuario(usuarioPedro)
                .nome("Pedro")
                .telefone("79966666666")
                .build());

        kaikeJoao = em.persist(ConversaEntity.builder()
                .cliente(kaike)
                .barbeiro(joao)
                .build());

        kaikePedro = em.persist(ConversaEntity.builder()
                .cliente(kaike)
                .barbeiro(pedro)
                .build());

        mariaJoao = em.persist(ConversaEntity.builder()
                .cliente(maria)
                .barbeiro(joao)
                .build());

        em.flush();
        em.clear();
    }

    @DisplayName("Salvar conversa")
    @Test
    void salvarConversa() {
        UsuarioEntity usuario = em.persist(UsuarioEntity.builder()
                .email("carlos@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        ClienteEntity carlos = em.persist(ClienteEntity.builder()
                .usuario(usuario)
                .nome("Carlos")
                .telefone("79955555555")
                .build());

        BarbeiroEntity barbeiro = em.find(BarbeiroEntity.class, joao.getId());

        ConversaEntity salva = repository.save(ConversaEntity.builder()
                .cliente(carlos)
                .barbeiro(barbeiro)
                .build());

        em.flush();
        em.clear();

        ConversaEntity doBanco = repository.findById(salva.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertEquals(carlos.getId(), doBanco.getCliente().getId());
        assertEquals(joao.getId(), doBanco.getBarbeiro().getId());
        assertFalse(doBanco.isEncerrada());
    }

    @DisplayName("Não permite duas conversas para o mesmo par cliente/barbeiro")
    @Test
    void naoPermiteConversaDuplicada() {
        ClienteEntity cliente = em.find(ClienteEntity.class, kaike.getId());
        BarbeiroEntity barbeiro = em.find(BarbeiroEntity.class, joao.getId());

        ConversaEntity duplicada = ConversaEntity.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(duplicada));
    }

    @DisplayName("Permite o mesmo cliente com barbeiros diferentes")
    @Test
    void permiteMesmoClienteComBarbeirosDiferentes() {
        List<ConversaEntity> conversas = repository.findByClienteId(kaike.getId());

        assertEquals(2, conversas.size());
    }

    @DisplayName("Não salva conversa sem cliente")
    @Test
    void naoSalvaSemCliente() {
        BarbeiroEntity barbeiro = em.find(BarbeiroEntity.class, pedro.getId());

        ConversaEntity semCliente = ConversaEntity.builder()
                .barbeiro(barbeiro)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semCliente));
    }

    @DisplayName("Não salva conversa sem barbeiro")
    @Test
    void naoSalvaSemBarbeiro() {
        ClienteEntity cliente = em.find(ClienteEntity.class, maria.getId());

        ConversaEntity semBarbeiro = ConversaEntity.builder()
                .cliente(cliente)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semBarbeiro));
    }

    @DisplayName("Conversa nasce não encerrada")
    @Test
    void conversaNasceNaoEncerrada() {
        ConversaEntity doBanco = repository.findById(kaikeJoao.getId()).orElseThrow();

        assertFalse(doBanco.isEncerrada());
    }

    @DisplayName("Encerrar conversa persiste a alteração")
    @Test
    void encerrarConversa() {
        ConversaEntity conversa = repository.findById(kaikeJoao.getId()).orElseThrow();
        conversa.setEncerrada(true);
        repository.saveAndFlush(conversa);
        em.clear();

        assertTrue(repository.findById(kaikeJoao.getId()).orElseThrow().isEncerrada());
        assertFalse(repository.findById(kaikePedro.getId()).orElseThrow().isEncerrada());
        assertFalse(repository.findById(mariaJoao.getId()).orElseThrow().isEncerrada());
    }

    @DisplayName("Listar")
    @Test
    void listar() {
        List<ConversaEntity> lista = repository.findAll();

        assertEquals(3, lista.size());
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {
        ConversaEntity doBanco = repository.findById(kaikeJoao.getId()).orElseThrow();

        assertEquals(kaikeJoao.getId(), doBanco.getId());
        assertEquals(kaike.getId(), doBanco.getCliente().getId());
        assertEquals(joao.getId(), doBanco.getBarbeiro().getId());
    }

    @DisplayName("Busca por Id que não existe")
    @Test
    void findByIdInexistente() {
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @DisplayName("Busca por cliente e barbeiro")
    @Test
    void findByClienteIdAndBarbeiroId() {
        Optional<ConversaEntity> resultado =
                repository.findByClienteIdAndBarbeiroId(kaike.getId(), pedro.getId());

        assertTrue(resultado.isPresent());
        assertEquals(kaikePedro.getId(), resultado.get().getId());
        assertEquals(kaike.getId(), resultado.get().getCliente().getId());
        assertEquals(pedro.getId(), resultado.get().getBarbeiro().getId());
    }

    @DisplayName("Busca por cliente e barbeiro sem conversa entre eles")
    @Test
    void findByClienteIdAndBarbeiroIdInexistente() {
        Optional<ConversaEntity> resultado =
                repository.findByClienteIdAndBarbeiroId(maria.getId(), pedro.getId());

        assertTrue(resultado.isEmpty());
    }

    @DisplayName("Busca por cliente e barbeiro não confunde os papéis")
    @Test
    void findByClienteIdAndBarbeiroIdIdsTrocados() {
        Optional<ConversaEntity> resultado =
                repository.findByClienteIdAndBarbeiroId(joao.getId(), kaike.getId());

        assertTrue(resultado.isEmpty());
    }

    @DisplayName("Busca conversas por cliente")
    @Test
    void findByClienteId() {
        List<ConversaEntity> resultado = repository.findByClienteId(kaike.getId());
        List<Long> ids = resultado.stream().map(ConversaEntity::getId).toList();

        assertEquals(2, resultado.size());
        assertTrue(ids.containsAll(List.of(kaikeJoao.getId(), kaikePedro.getId())));
        assertFalse(ids.contains(mariaJoao.getId()));
        resultado.forEach(c -> assertEquals(kaike.getId(), c.getCliente().getId()));
    }

    @DisplayName("Cliente sem conversas devolve lista vazia")
    @Test
    void findByClienteIdSemConversas() {
        UsuarioEntity usuario = em.persist(UsuarioEntity.builder()
                .email("sozinho@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        ClienteEntity sozinho = em.persist(ClienteEntity.builder()
                .usuario(usuario)
                .nome("Sozinho")
                .telefone("79922222222")
                .build());

        em.flush();

        assertTrue(repository.findByClienteId(sozinho.getId()).isEmpty());
    }

    @DisplayName("Busca conversas por barbeiro")
    @Test
    void findByBarbeiroId() {
        List<ConversaEntity> resultado = repository.findByBarbeiroId(joao.getId());
        List<Long> ids = resultado.stream().map(ConversaEntity::getId).toList();

        assertEquals(2, resultado.size());
        assertTrue(ids.containsAll(List.of(kaikeJoao.getId(), mariaJoao.getId())));
        assertFalse(ids.contains(kaikePedro.getId()));
        resultado.forEach(c -> assertEquals(joao.getId(), c.getBarbeiro().getId()));
    }

    @DisplayName("Barbeiro sem conversas devolve lista vazia")
    @Test
    void findByBarbeiroIdSemConversas() {
        assertTrue(repository.findByBarbeiroId(9999L).isEmpty());
    }

    @DisplayName("Deletar conversa")
    @Test
    void deletar() {
        repository.deleteById(kaikeJoao.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(kaikeJoao.getId()).isEmpty());
        assertTrue(repository.findById(kaikePedro.getId()).isPresent());
        assertTrue(repository.findById(mariaJoao.getId()).isPresent());
    }
}