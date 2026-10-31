package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.HistoricoConversaEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class HistoricoRepositoryTest {

    @Autowired
    private HistoricoRepository repository;

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

    private HistoricoConversaEntity criarMensagem(String msg, LocalDateTime data, ClienteEntity cliente) {
        return em.persist(HistoricoConversaEntity.builder()
                .msg(msg)
                .msgDate(data)
                .role("user")
                .cliente(cliente)
                .build());
    }

    @DisplayName("Salvar mensagem")
    @Test
    void salvarMensagem() {
        HistoricoConversaEntity salvo = repository.save(HistoricoConversaEntity.builder()
                .msg("Quero agendar um corte")
                .msgDate(LocalDateTime.of(2027, 2, 12, 9, 0))
                .role("user")
                .cliente(kaike)
                .build());

        HistoricoConversaEntity doBanco = repository.findById(salvo.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertEquals("Quero agendar um corte", doBanco.getMsg());
        assertEquals("user", doBanco.getRole());
        assertEquals(kaike.getId(), doBanco.getCliente().getId());
    }

    @DisplayName("Busca por cliente, do mais antigo para o mais recente")
    @Test
    void findByClienteOrderByMsgDateAsc() {
        criarMensagem("terceira", LocalDateTime.of(2027, 2, 12, 9, 30), kaike);
        criarMensagem("primeira", LocalDateTime.of(2027, 2, 12, 9, 0), kaike);
        criarMensagem("da maria", LocalDateTime.of(2027, 2, 12, 9, 15), maria);
        criarMensagem("segunda", LocalDateTime.of(2027, 2, 12, 9, 10), kaike);

        List<HistoricoConversaEntity> resultado = repository.findByClienteOrderByMsgDateAsc(kaike);
        List<String> mensagens = resultado.stream().map(HistoricoConversaEntity::getMsg).toList();

        assertEquals(List.of("primeira", "segunda", "terceira"), mensagens);
    }

    @DisplayName("Cliente sem mensagens devolve lista vazia")
    @Test
    void findByClienteSemMensagens() {
        criarMensagem("da maria", LocalDateTime.of(2027, 2, 12, 9, 15), maria);

        assertTrue(repository.findByClienteOrderByMsgDateAsc(kaike).isEmpty());
    }

    @DisplayName("Deletar mensagem")
    @Test
    void deletar() {
        HistoricoConversaEntity msg1 = criarMensagem("apagar", LocalDateTime.of(2027, 2, 12, 9, 0), kaike);
        HistoricoConversaEntity msg2 = criarMensagem("manter", LocalDateTime.of(2027, 2, 12, 9, 5), kaike);

        repository.deleteById(msg1.getId());

        assertTrue(repository.findById(msg1.getId()).isEmpty());
        assertTrue(repository.findById(msg2.getId()).isPresent());
    }
}