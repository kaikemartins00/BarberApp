package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
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
class ServicoRepositoryTest {

    @Autowired
    private ServicoRepository repository;

    @Autowired
    private TestEntityManager em;

    private ServicoEntity corte;
    private ServicoEntity barba;

    @BeforeEach
    void setUp() {
        corte = em.persist(ServicoEntity.builder().nome("Corte").preco(40.00).build());
        barba = em.persist(ServicoEntity.builder().nome("Barba").preco(30.00).build());

        em.flush();
        em.clear();
    }

    @DisplayName("Salvar serviço")
    @Test
    void salvarServico() {
        ServicoEntity salvo = repository.save(ServicoEntity.builder()
                .nome("Sobrancelha")
                .preco(15.50)
                .build());

        em.flush();
        em.clear();

        ServicoEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertEquals("Sobrancelha", doBanco.getNome());
        assertEquals(15.50, doBanco.getPreco(), 0.001);
    }

    @DisplayName("Não salva serviço sem nome")
    @Test
    void naoSalvaSemNome() {
        ServicoEntity semNome = ServicoEntity.builder().preco(10.00).build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semNome));
    }

    @DisplayName("Atualizar serviço persiste a alteração")
    @Test
    void atualizarServico() {
        ServicoEntity servico = repository.findById(corte.getId()).orElseThrow();
        servico.setPreco(45.00);
        repository.saveAndFlush(servico);
        em.clear();

        ServicoEntity doBanco = repository.findById(corte.getId()).orElseThrow();

        assertEquals(45.00, doBanco.getPreco(), 0.001);
        assertEquals(30.00, repository.findById(barba.getId()).orElseThrow().getPreco(), 0.001);
    }

    @DisplayName("Listar")
    @Test
    void listar() {
        List<ServicoEntity> lista = repository.findAll();
        List<String> nomes = lista.stream().map(ServicoEntity::getNome).toList();

        assertEquals(2, lista.size());
        assertTrue(nomes.containsAll(List.of("Corte", "Barba")));
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {
        ServicoEntity doBanco = repository.findById(corte.getId()).orElseThrow();

        assertEquals(corte.getId(), doBanco.getId());
        assertEquals("Corte", doBanco.getNome());
        assertEquals(40.00, doBanco.getPreco(), 0.001);
    }

    @DisplayName("Busca por Id que não existe")
    @Test
    void findByIdInexistente() {
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @DisplayName("Busca por nome ignorando maiúsculas")
    @Test
    void findByNomeIgnoreCase() {
        Optional<ServicoEntity> maiusculo = repository.findByNomeIgnoreCase("CORTE");
        Optional<ServicoEntity> minusculo = repository.findByNomeIgnoreCase("corte");

        assertTrue(maiusculo.isPresent());
        assertTrue(minusculo.isPresent());
        assertEquals(corte.getId(), maiusculo.get().getId());
        assertEquals(corte.getId(), minusculo.get().getId());
    }

    @DisplayName("Busca por nome que não existe")
    @Test
    void findByNomeIgnoreCaseInexistente() {
        assertTrue(repository.findByNomeIgnoreCase("Hidratação").isEmpty());
    }

    @DisplayName("Existe por nome ignorando maiúsculas")
    @Test
    void existsByNomeIgnoreCase() {
        assertTrue(repository.existsByNomeIgnoreCase("BARBA"));
        assertTrue(repository.existsByNomeIgnoreCase("barba"));
    }

    @DisplayName("Deletar serviço")
    @Test
    void deletar() {
        repository.deleteById(corte.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(corte.getId()).isEmpty());
        assertTrue(repository.findById(barba.getId()).isPresent());
    }
}