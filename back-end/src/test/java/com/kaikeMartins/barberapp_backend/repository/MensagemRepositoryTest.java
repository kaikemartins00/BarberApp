package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ConversaEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.MensagemEntity;
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
class MensagemRepositoryTest {

    @Autowired
    private MensagemRepository repository;

    @Autowired
    private TestEntityManager em;

    private UsuarioEntity usuarioKaike;
    private UsuarioEntity usuarioJoao;
    private UsuarioEntity usuarioMaria;

    private ConversaEntity conversaKaikeJoao;
    private ConversaEntity conversaMariaJoao;
    private ConversaEntity conversaVazia;

    private MensagemEntity m1;
    private MensagemEntity m2;
    private MensagemEntity m3;
    private MensagemEntity m4;
    private MensagemEntity m5;

    private MensagemEntity criarMensagem(ConversaEntity conversa, UsuarioEntity remetente,
                                         String texto, boolean lida) {
        return em.persist(MensagemEntity.builder()
                .conversa(conversa)
                .remetente(remetente)
                .texto(texto)
                .lida(lida)
                .build());
    }

    @BeforeEach
    void setUp() {
        usuarioKaike = em.persist(UsuarioEntity.builder()
                .email("kaike@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        usuarioJoao = em.persist(UsuarioEntity.builder()
                .email("joao@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        usuarioMaria = em.persist(UsuarioEntity.builder()
                .email("maria@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        UsuarioEntity usuarioPedro = em.persist(UsuarioEntity.builder()
                .email("pedro@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        ClienteEntity kaike = em.persist(ClienteEntity.builder()
                .usuario(usuarioKaike)
                .nome("Kaike")
                .telefone("79999999999")
                .build());

        ClienteEntity maria = em.persist(ClienteEntity.builder()
                .usuario(usuarioMaria)
                .nome("Maria")
                .telefone("79977777777")
                .build());

        BarbeiroEntity joao = em.persist(BarbeiroEntity.builder()
                .usuario(usuarioJoao)
                .nome("Joao")
                .telefone("79988888888")
                .build());

        BarbeiroEntity pedro = em.persist(BarbeiroEntity.builder()
                .usuario(usuarioPedro)
                .nome("Pedro")
                .telefone("79966666666")
                .build());

        conversaKaikeJoao = em.persist(ConversaEntity.builder()
                .cliente(kaike)
                .barbeiro(joao)
                .build());

        conversaMariaJoao = em.persist(ConversaEntity.builder()
                .cliente(maria)
                .barbeiro(joao)
                .build());

        conversaVazia = em.persist(ConversaEntity.builder()
                .cliente(kaike)
                .barbeiro(pedro)
                .build());

        m1 = criarMensagem(conversaKaikeJoao, usuarioKaike, "oi", false);
        m2 = criarMensagem(conversaKaikeJoao, usuarioJoao, "ola", false);
        m3 = criarMensagem(conversaKaikeJoao, usuarioKaike, "tudo bem?", false);
        m4 = criarMensagem(conversaKaikeJoao, usuarioJoao, "sim", true);
        m5 = criarMensagem(conversaMariaJoao, usuarioMaria, "outra conversa", false);

        em.flush();
        em.clear();
    }

    @DisplayName("Salvar mensagem preenche a data de envio automaticamente")
    @Test
    void salvarMensagem() {
        ConversaEntity conversa = em.find(ConversaEntity.class, conversaKaikeJoao.getId());
        UsuarioEntity remetente = em.find(UsuarioEntity.class, usuarioKaike.getId());

        MensagemEntity salva = repository.save(MensagemEntity.builder()
                .conversa(conversa)
                .remetente(remetente)
                .texto("nova mensagem")
                .lida(false)
                .build());

        em.flush();
        em.clear();

        MensagemEntity doBanco = repository.findById(salva.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertNotNull(doBanco.getEnviadaEm());
        assertEquals("nova mensagem", doBanco.getTexto());
        assertFalse(doBanco.isLida());
        assertEquals(conversaKaikeJoao.getId(), doBanco.getConversa().getId());
        assertEquals(usuarioKaike.getId(), doBanco.getRemetente().getId());
    }

    @DisplayName("Não salva mensagem sem texto")
    @Test
    void naoSalvaSemTexto() {
        ConversaEntity conversa = em.find(ConversaEntity.class, conversaKaikeJoao.getId());
        UsuarioEntity remetente = em.find(UsuarioEntity.class, usuarioKaike.getId());

        MensagemEntity semTexto = MensagemEntity.builder()
                .conversa(conversa)
                .remetente(remetente)
                .lida(false)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semTexto));
    }

    @DisplayName("Não salva mensagem sem remetente")
    @Test
    void naoSalvaSemRemetente() {
        ConversaEntity conversa = em.find(ConversaEntity.class, conversaKaikeJoao.getId());

        MensagemEntity semRemetente = MensagemEntity.builder()
                .conversa(conversa)
                .texto("sem remetente")
                .lida(false)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semRemetente));
    }

    @DisplayName("Não salva mensagem sem conversa")
    @Test
    void naoSalvaSemConversa() {
        UsuarioEntity remetente = em.find(UsuarioEntity.class, usuarioKaike.getId());

        MensagemEntity semConversa = MensagemEntity.builder()
                .remetente(remetente)
                .texto("sem conversa")
                .lida(false)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semConversa));
    }

    @DisplayName("Listar")
    @Test
    void listar() {
        assertEquals(5, repository.findAll().size());
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {
        MensagemEntity doBanco = repository.findById(m2.getId()).orElseThrow();

        assertEquals(m2.getId(), doBanco.getId());
        assertEquals("ola", doBanco.getTexto());
        assertEquals(usuarioJoao.getId(), doBanco.getRemetente().getId());
        assertEquals(conversaKaikeJoao.getId(), doBanco.getConversa().getId());
    }

    @DisplayName("Busca por Id que não existe")
    @Test
    void findByIdInexistente() {
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @DisplayName("Busca mensagens depois de um id em ordem crescente")
    @Test
    void findByConversaIdAndIdGreaterThanOrderByIdAsc() {
        List<MensagemEntity> resultado =
                repository.findByConversaIdAndIdGreaterThanOrderByIdAsc(conversaKaikeJoao.getId(), m1.getId());
        List<Long> ids = resultado.stream().map(MensagemEntity::getId).toList();

        assertEquals(List.of(m2.getId(), m3.getId(), m4.getId()), ids);
    }

    @DisplayName("Busca desde o início com id zero")
    @Test
    void findByConversaIdAndIdGreaterThanDesdeInicio() {
        List<MensagemEntity> resultado =
                repository.findByConversaIdAndIdGreaterThanOrderByIdAsc(conversaKaikeJoao.getId(), 0L);
        List<Long> ids = resultado.stream().map(MensagemEntity::getId).toList();

        assertEquals(List.of(m1.getId(), m2.getId(), m3.getId(), m4.getId()), ids);
    }

    @DisplayName("Não retorna mensagens de outra conversa")
    @Test
    void findByConversaIdAndIdGreaterThanNaoMisturaConversas() {
        List<MensagemEntity> resultado =
                repository.findByConversaIdAndIdGreaterThanOrderByIdAsc(conversaMariaJoao.getId(), 0L);

        assertEquals(1, resultado.size());
        assertEquals(m5.getId(), resultado.get(0).getId());
    }

    @DisplayName("Sem mensagens depois do último id")
    @Test
    void findByConversaIdAndIdGreaterThanVazio() {
        assertTrue(repository.findByConversaIdAndIdGreaterThanOrderByIdAsc(
                conversaKaikeJoao.getId(), m4.getId()).isEmpty());
    }

    @DisplayName("Busca a última mensagem da conversa")
    @Test
    void findFirstByConversaIdOrderByIdDesc() {
        Optional<MensagemEntity> ultima =
                repository.findFirstByConversaIdOrderByIdDesc(conversaKaikeJoao.getId());

        assertTrue(ultima.isPresent());
        assertEquals(m4.getId(), ultima.get().getId());
        assertEquals("sim", ultima.get().getTexto());
        assertNotNull(ultima.get().getEnviadaEm());
    }

    @DisplayName("Última mensagem de conversa sem mensagens")
    @Test
    void findFirstByConversaIdOrderByIdDescVazio() {
        assertTrue(repository.findFirstByConversaIdOrderByIdDesc(conversaVazia.getId()).isEmpty());
    }

    @DisplayName("Conta não lidas que não foram enviadas pelo usuário (kaike)")
    @Test
    void countNaoLidasParaKaike() {
        long total = repository.countByConversaIdAndRemetenteIdNotAndLidaFalse(
                conversaKaikeJoao.getId(), usuarioKaike.getId());

        assertEquals(1, total);
    }

    @DisplayName("Conta não lidas que não foram enviadas pelo usuário (joao)")
    @Test
    void countNaoLidasParaJoao() {
        long total = repository.countByConversaIdAndRemetenteIdNotAndLidaFalse(
                conversaKaikeJoao.getId(), usuarioJoao.getId());

        assertEquals(2, total);
    }

    @DisplayName("Contagem ignora outras conversas")
    @Test
    void countNaoLidasIgnoraOutrasConversas() {
        long total = repository.countByConversaIdAndRemetenteIdNotAndLidaFalse(
                conversaMariaJoao.getId(), usuarioJoao.getId());

        assertEquals(1, total);
    }

    @DisplayName("Contagem zero em conversa sem mensagens")
    @Test
    void countNaoLidasConversaVazia() {
        assertEquals(0, repository.countByConversaIdAndRemetenteIdNotAndLidaFalse(
                conversaVazia.getId(), usuarioKaike.getId()));
    }

    @DisplayName("Marca como lidas apenas as mensagens do outro usuário na conversa")
    @Test
    void marcarComoLidas() {
        int atualizadas = repository.marcarComoLidas(conversaKaikeJoao.getId(), usuarioKaike.getId());

        em.flush();
        em.clear();

        assertEquals(1, atualizadas);
        assertTrue(repository.findById(m2.getId()).orElseThrow().isLida());
        assertFalse(repository.findById(m1.getId()).orElseThrow().isLida());
        assertFalse(repository.findById(m3.getId()).orElseThrow().isLida());
        assertTrue(repository.findById(m4.getId()).orElseThrow().isLida());
        assertFalse(repository.findById(m5.getId()).orElseThrow().isLida());
    }

    @DisplayName("Marcar como lidas zera a contagem de não lidas")
    @Test
    void marcarComoLidasZeraContagem() {
        int atualizadas = repository.marcarComoLidas(conversaKaikeJoao.getId(), usuarioJoao.getId());

        em.flush();
        em.clear();

        assertEquals(2, atualizadas);
        assertEquals(0, repository.countByConversaIdAndRemetenteIdNotAndLidaFalse(
                conversaKaikeJoao.getId(), usuarioJoao.getId()));
    }

    @DisplayName("Marcar como lidas não altera outra conversa")
    @Test
    void marcarComoLidasNaoAlteraOutraConversa() {
        repository.marcarComoLidas(conversaKaikeJoao.getId(), usuarioJoao.getId());

        em.flush();
        em.clear();

        assertEquals(1, repository.countByConversaIdAndRemetenteIdNotAndLidaFalse(
                conversaMariaJoao.getId(), usuarioJoao.getId()));
    }

    @DisplayName("Marcar como lidas é idempotente")
    @Test
    void marcarComoLidasSegundaVez() {
        repository.marcarComoLidas(conversaKaikeJoao.getId(), usuarioKaike.getId());
        em.flush();
        em.clear();

        int segundaVez = repository.marcarComoLidas(conversaKaikeJoao.getId(), usuarioKaike.getId());

        assertEquals(0, segundaVez);
    }

    @DisplayName("Marcar como lidas em conversa sem mensagens")
    @Test
    void marcarComoLidasConversaVazia() {
        assertEquals(0, repository.marcarComoLidas(conversaVazia.getId(), usuarioKaike.getId()));
    }

    @DisplayName("Deletar mensagem")
    @Test
    void deletar() {
        repository.deleteById(m1.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(m1.getId()).isEmpty());
        assertTrue(repository.findById(m2.getId()).isPresent());
    }
}