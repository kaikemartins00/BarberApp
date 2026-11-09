package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
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
class BarbeiroRepositoryTest {

    @Autowired
    private BarbeiroRepository repository;

    @Autowired
    private TestEntityManager em;

    private BarbeiroEntity joao;
    private BarbeiroEntity pedro;

    @BeforeEach
    void setUp() {

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
    }

    @DisplayName("Salvar barbeiro")
    @Test
    void salvarBarbeiro() {

        UsuarioEntity usuario = em.persist(UsuarioEntity.builder()
                .email("carlos@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        BarbeiroEntity novo = BarbeiroEntity.builder()
                .usuario(usuario)
                .nome("Carlos")
                .telefone("79955555555")
                .fotoUrl("http://foto.com/carlos.jpg")
                .build();

        BarbeiroEntity salvo = repository.save(novo);
        em.flush();
        em.clear();

        BarbeiroEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertEquals(usuario.getId(), doBanco.getUsuario().getId());
        assertEquals("Carlos", doBanco.getNome());
        assertEquals("79955555555", doBanco.getTelefone());
        assertEquals("http://foto.com/carlos.jpg", doBanco.getFotoUrl());
    }

    @DisplayName("Não salva barbeiro sem nome")
    @Test
    void naoSalvaSemNome() {

        UsuarioEntity usuario = em.persist(UsuarioEntity.builder()
                .email("semnome@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        BarbeiroEntity semNome = BarbeiroEntity.builder()
                .usuario(usuario)
                .telefone("79944444444")
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semNome));
    }

    @DisplayName("Listar")
    @Test
    void listar() {

        List<BarbeiroEntity> lista = repository.findAll();
        List<String> nomes = lista.stream().map(BarbeiroEntity::getNome).toList();

        assertEquals(2, lista.size());
        assertTrue(nomes.containsAll(List.of("Joao", "Pedro")));
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {

        repository.save(joao);

        BarbeiroEntity busca = repository.findById(joao.getId()).get();

        assertNotNull(busca);
        assertEquals(joao.getId(), busca.getId());
    }

    @DisplayName("Busca por Id que não existe")
    @Test
    void findByIdInexistente() {
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @DisplayName("Busca por email do usuário")
    @Test
    void findByUsuarioEmail() {

        Optional<BarbeiroEntity> resultado = repository.findByUsuarioEmail("pedro@email.com");

        assertTrue(resultado.isPresent());
        assertEquals(pedro.getId(), resultado.get().getId());
        assertEquals("Pedro", resultado.get().getNome());
    }

    @DisplayName("Busca por email que não existe")
    @Test
    void findByUsuarioEmailInexistente() {

        Optional<BarbeiroEntity> resultado = repository.findByUsuarioEmail("naoexiste@email.com");

        assertTrue(resultado.isEmpty());
    }

    @DisplayName("Busca por nome ignorando maiúsculas")
    @Test
    void findByNomeIgnoreCase() {

        Optional<BarbeiroEntity> maiusculo = repository.findByNomeIgnoreCase("JOAO");
        Optional<BarbeiroEntity> minusculo = repository.findByNomeIgnoreCase("joao");

        assertTrue(maiusculo.isPresent());
        assertTrue(minusculo.isPresent());
        assertEquals(joao.getId(), maiusculo.get().getId());
        assertEquals(joao.getId(), minusculo.get().getId());
    }

    @DisplayName("Busca por nome que não existe")
    @Test
    void findByNomeIgnoreCaseInexistente() {

        assertTrue(repository.findByNomeIgnoreCase("Fulano").isEmpty());
    }

    @DisplayName("Busca barbeiros por serviço")
    @Test
    void findByServicosId() {

        ServicoEntity corte = em.persist(ServicoEntity.builder()
                .nome("corte")
                .preco(40.00)
                .build());

        ServicoEntity barba = em.persist(ServicoEntity.builder()
                .nome("barba")
                .preco(25.00)
                .build());

        joao.getServicos().add(corte);
        joao.getServicos().add(barba);
        pedro.getServicos().add(corte);

        em.persistAndFlush(joao);
        em.persistAndFlush(pedro);
        em.clear();

        List<BarbeiroEntity> fazemCorte = repository.findByServicosId(corte.getId());
        List<BarbeiroEntity> fazemBarba = repository.findByServicosId(barba.getId());

        assertEquals(2, fazemCorte.size());
        assertEquals(1, fazemBarba.size());
        assertEquals(joao.getId(), fazemBarba.get(0).getId());
        assertTrue(repository.findByServicosId(9999L).isEmpty());
    }

    @DisplayName("Deletar barbeiro")
    @Test
    void deletar() {

        repository.deleteById(joao.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(joao.getId()).isEmpty());
        assertTrue(repository.findById(pedro.getId()).isPresent());
    }
}