package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.RoleEntity;
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
class RoleRepositoryTest {

    @Autowired
    private RoleRepository repository;

    @Autowired
    private TestEntityManager em;

    private RoleEntity admin;
    private RoleEntity cliente;

    @BeforeEach
    void setUp() {
        admin = em.persist(RoleEntity.builder().name("ROLE_ADMIN").build());
        cliente = em.persist(RoleEntity.builder().name("ROLE_CLIENTE").build());

        em.flush();
        em.clear();
    }

    @DisplayName("Salvar role")
    @Test
    void salvarRole() {
        RoleEntity salvo = repository.save(RoleEntity.builder().name("ROLE_BARBEIRO").build());

        em.flush();
        em.clear();

        RoleEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertEquals("ROLE_BARBEIRO", doBanco.getName());
    }

    @DisplayName("Listar")
    @Test
    void listar() {
        List<RoleEntity> lista = repository.findAll();
        List<String> nomes = lista.stream().map(RoleEntity::getName).toList();

        assertEquals(2, lista.size());
        assertTrue(nomes.containsAll(List.of("ROLE_ADMIN", "ROLE_CLIENTE")));
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {
        RoleEntity doBanco = repository.findById(admin.getId()).orElseThrow();

        assertEquals(admin.getId(), doBanco.getId());
        assertEquals("ROLE_ADMIN", doBanco.getName());
    }

    @DisplayName("Busca por Id que não existe")
    @Test
    void findByIdInexistente() {
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @DisplayName("Busca por nome")
    @Test
    void findByName() {
        Optional<RoleEntity> resultado = repository.findByName("ROLE_CLIENTE");

        assertTrue(resultado.isPresent());
        assertEquals(cliente.getId(), resultado.get().getId());
        assertEquals("ROLE_CLIENTE", resultado.get().getName());
    }

    @DisplayName("Busca por nome que não existe")
    @Test
    void findByNameInexistente() {
        assertTrue(repository.findByName("ROLE_FANTASMA").isEmpty());
    }

    @DisplayName("Busca por nome diferencia maiúsculas")
    @Test
    void findByNameCaseSensitive() {
        assertTrue(repository.findByName("role_admin").isEmpty());
    }

    @DisplayName("Deletar role")
    @Test
    void deletar() {
        repository.deleteById(admin.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(admin.getId()).isEmpty());
        assertTrue(repository.findById(cliente.getId()).isPresent());
    }
}