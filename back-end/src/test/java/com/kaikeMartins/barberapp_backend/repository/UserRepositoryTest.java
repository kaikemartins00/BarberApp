package com.kaikeMartins.barberapp_backend.repository;

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
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private TestEntityManager em;

    private UsuarioEntity kaike;
    private UsuarioEntity maria;

    @BeforeEach
    void setUp() {
        kaike = em.persist(UsuarioEntity.builder()
                .email("kaike@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        maria = em.persist(UsuarioEntity.builder()
                .email("maria@email.com")
                .senha("654321")
                .verificado(false)
                .build());

        em.flush();
        em.clear();
    }

    @DisplayName("Salvar usuário")
    @Test
    void salvarUsuario() {
        UsuarioEntity novo = UsuarioEntity.builder()
                .email("carlos@email.com")
                .senha("abc123")
                .verificado(true)
                .build();

        UsuarioEntity salvo = repository.save(novo);

        em.flush();
        em.clear();

        UsuarioEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertEquals("carlos@email.com", doBanco.getEmail());
        assertEquals("abc123", doBanco.getSenha());
        assertTrue(doBanco.isVerificado());
    }

    @DisplayName("Não salva email repetido")
    @Test
    void naoSalvaEmailRepetido() {
        UsuarioEntity repetido = UsuarioEntity.builder()
                .email("kaike@email.com")
                .senha("outra")
                .verificado(true)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(repetido));
    }

    @DisplayName("Não salva usuário sem email")
    @Test
    void naoSalvaSemEmail() {
        UsuarioEntity semEmail = UsuarioEntity.builder()
                .senha("123456")
                .verificado(true)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semEmail));
    }

    @DisplayName("Não salva usuário sem senha")
    @Test
    void naoSalvaSemSenha() {
        UsuarioEntity semSenha = UsuarioEntity.builder()
                .email("semsenha@email.com")
                .verificado(true)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semSenha));
    }

    @DisplayName("Atualizar verificado persiste a alteração")
    @Test
    void atualizarVerificado() {
        UsuarioEntity usuario = repository.findById(maria.getId()).orElseThrow();
        usuario.setVerificado(true);
        repository.saveAndFlush(usuario);
        em.clear();

        assertTrue(repository.findById(maria.getId()).orElseThrow().isVerificado());
        assertTrue(repository.findById(kaike.getId()).orElseThrow().isVerificado());
    }

    @DisplayName("Listar")
    @Test
    void listar() {
        List<UsuarioEntity> lista = repository.findAll();
        List<String> emails = lista.stream().map(UsuarioEntity::getEmail).toList();

        assertEquals(2, lista.size());
        assertTrue(emails.containsAll(List.of("kaike@email.com", "maria@email.com")));
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {
        UsuarioEntity doBanco = repository.findById(kaike.getId()).orElseThrow();

        assertEquals(kaike.getId(), doBanco.getId());
        assertEquals("kaike@email.com", doBanco.getEmail());
        assertTrue(doBanco.isVerificado());
    }

    @DisplayName("Busca por Id que não existe")
    @Test
    void findByIdInexistente() {
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @DisplayName("Busca por email")
    @Test
    void findByEmail() {
        Optional<UsuarioEntity> resultado = repository.findByEmail("maria@email.com");

        assertTrue(resultado.isPresent());
        assertEquals(maria.getId(), resultado.get().getId());
        assertEquals("654321", resultado.get().getSenha());
        assertFalse(resultado.get().isVerificado());
    }

    @DisplayName("Busca por email que não existe")
    @Test
    void findByEmailInexistente() {
        assertTrue(repository.findByEmail("naoexiste@email.com").isEmpty());
    }

    @DisplayName("Busca por email diferencia maiúsculas")
    @Test
    void findByEmailCaseSensitive() {
        assertTrue(repository.findByEmail("KAIKE@EMAIL.COM").isEmpty());
    }

    @DisplayName("Existe por email")
    @Test
    void existsByEmail() {
        assertTrue(repository.existsByEmail("kaike@email.com"));
        assertTrue(repository.existsByEmail("maria@email.com"));
    }

    @DisplayName("Não existe por email inexistente")
    @Test
    void existsByEmailInexistente() {
        assertFalse(repository.existsByEmail("naoexiste@email.com"));
    }

    @DisplayName("Deletar usuário")
    @Test
    void deletar() {
        repository.deleteById(kaike.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(kaike.getId()).isEmpty());
        assertTrue(repository.findById(maria.getId()).isPresent());
    }
}