package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
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
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

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

    @DisplayName("Salvar cliente")
    @Test
    void salvarCliente() {
        UsuarioEntity usuario = em.persist(UsuarioEntity.builder()
                .email("carlos@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        ClienteEntity novo = ClienteEntity.builder()
                .usuario(usuario)
                .nome("Carlos")
                .telefone("79955555555")
                .build();

        ClienteEntity salvo = repository.save(novo);
        em.flush();
        em.clear();

        ClienteEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertEquals(usuario.getId(), doBanco.getUsuario().getId());
        assertEquals("Carlos", doBanco.getNome());
        assertEquals("79955555555", doBanco.getTelefone());
    }

    @DisplayName("Não salva cliente sem nome")
    @Test
    void naoSalvaSemNome() {
        UsuarioEntity usuario = em.persist(UsuarioEntity.builder()
                .email("semnome@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        ClienteEntity semNome = ClienteEntity.builder()
                .usuario(usuario)
                .telefone("79944444444")
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semNome));
    }

    @DisplayName("Não salva cliente sem usuário")
    @Test
    void naoSalvaSemUsuario() {
        ClienteEntity semUsuario = ClienteEntity.builder()
                .nome("Sem Usuario")
                .telefone("79933333333")
                .build();

        assertThrows(Exception.class,
                () -> repository.saveAndFlush(semUsuario));
    }

    @DisplayName("Não permite dois clientes para o mesmo usuário")
    @Test
    void naoPermiteDoisClientesParaMesmoUsuario() {
        UsuarioEntity usuarioKaike = kaike.getUsuario();

        ClienteEntity duplicado = ClienteEntity.builder()
                .usuario(usuarioKaike)
                .nome("Kaike 2")
                .telefone("79911111111")
                .build();

        assertThrows(Exception.class,
                () -> repository.saveAndFlush(duplicado));
    }

    @DisplayName("Listar")
    @Test
    void listar() {
        em.flush();
        em.clear();

        List<ClienteEntity> lista = repository.findAll();
        List<String> nomes = lista.stream().map(ClienteEntity::getNome).toList();

        assertEquals(2, lista.size());
        assertTrue(nomes.containsAll(List.of("Kaike", "Maria")));
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {

        ClienteEntity doBanco = repository.findById(kaike.getId()).get();

        assertNotNull(doBanco);
        assertEquals(kaike.getId(), doBanco.getId());;
    }

    @DisplayName("Busca por Id que não existe")
    @Test
    void findByIdInexistente() {
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @DisplayName("Busca por email do usuário")
    @Test
    void findByUsuarioEmail() {

        Optional<ClienteEntity> resultado = repository.findByUsuarioEmail("maria@email.com");

        assertTrue(resultado.isPresent());
        assertEquals(maria.getId(), resultado.get().getId());
        assertEquals("Maria", resultado.get().getNome());
    }

    @DisplayName("Busca por email que não existe")
    @Test
    void findByUsuarioEmailInexistente() {
        assertTrue(repository.findByUsuarioEmail("naoexiste@email.com").isEmpty());
    }

    @DisplayName("Existe por email do usuário")
    @Test
    void existsByUsuarioEmail() {
        assertTrue(repository.existsByUsuarioEmail("kaike@email.com"));
        assertTrue(repository.existsByUsuarioEmail("maria@email.com"));
    }

    @DisplayName("Não existe por email inexistente")
    @Test
    void existsByUsuarioEmailInexistente() {
        assertFalse(repository.existsByUsuarioEmail("naoexiste@email.com"));
    }

    @DisplayName("Deletar cliente")
    @Test
    void deletar() {
        repository.deleteById(kaike.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(kaike.getId()).isEmpty());
        assertTrue(repository.findById(maria.getId()).isPresent());
    }
}