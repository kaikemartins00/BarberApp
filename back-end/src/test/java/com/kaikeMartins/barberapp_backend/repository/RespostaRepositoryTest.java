package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ChamadoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.RespostaChamadoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RespostaRepositoryTest {

    @Autowired
    private RespostaRepository repository;

    @Autowired
    private TestEntityManager em;

    private ChamadoEntity chamado;
    private BarbeiroEntity joao;
    private RespostaChamadoEntity r1;
    private RespostaChamadoEntity r2;

    private RespostaChamadoEntity criarResposta(String msg, BarbeiroEntity barbeiro) {
        return em.persist(RespostaChamadoEntity.builder()
                .respostaMsg(msg)
                .barbeiro(barbeiro)
                .chamado(chamado)
                .build());
    }

    @BeforeEach
    void setUp() {
        UsuarioEntity usuarioKaike = em.persist(UsuarioEntity.builder()
                .email("kaike@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        UsuarioEntity usuarioJoao = em.persist(UsuarioEntity.builder()
                .email("joao@email.com")
                .senha("123456")
                .verificado(true)
                .build());

        ClienteEntity kaike = em.persist(ClienteEntity.builder()
                .usuario(usuarioKaike)
                .nome("Kaike")
                .telefone("79999999999")
                .build());

        joao = em.persist(BarbeiroEntity.builder()
                .usuario(usuarioJoao)
                .nome("Joao")
                .telefone("79988888888")
                .build());

        chamado = em.persist(ChamadoEntity.builder()
                .titulo("Sem agua quente")
                .descricao("O chuveiro nao esquenta")
                .status(ChamadoStatus.ABERTO)
                .prioridade(PrioridadeChamado.MEDIA)
                .cliente(kaike)
                .build());

        r1 = criarResposta("primeira resposta", joao);
        r2 = criarResposta("segunda resposta", joao);

        em.flush();
        em.clear();
    }

    @DisplayName("Salvar resposta preenche a data de envio automaticamente")
    @Test
    void salvarResposta() {
        ChamadoEntity chamadoDoBanco = em.find(ChamadoEntity.class, chamado.getId());
        BarbeiroEntity barbeiro = em.find(BarbeiroEntity.class, joao.getId());

        RespostaChamadoEntity salva = repository.save(RespostaChamadoEntity.builder()
                .respostaMsg("nova resposta")
                .barbeiro(barbeiro)
                .chamado(chamadoDoBanco)
                .build());

        em.flush();
        em.clear();

        RespostaChamadoEntity doBanco = repository.findById(salva.getId()).orElseThrow();

        assertNotNull(doBanco.getId());
        assertNotNull(doBanco.getDataEnvio());
        assertEquals("nova resposta", doBanco.getRespostaMsg());
        assertEquals(joao.getId(), doBanco.getBarbeiro().getId());
        assertEquals(chamado.getId(), doBanco.getChamado().getId());
    }

    @DisplayName("Salva resposta sem barbeiro")
    @Test
    void salvaSemBarbeiro() {
        ChamadoEntity chamadoDoBanco = em.find(ChamadoEntity.class, chamado.getId());

        RespostaChamadoEntity salva = repository.save(RespostaChamadoEntity.builder()
                .respostaMsg("resposta do cliente")
                .chamado(chamadoDoBanco)
                .build());

        em.flush();
        em.clear();

        RespostaChamadoEntity doBanco = repository.findById(salva.getId()).orElseThrow();

        assertNull(doBanco.getBarbeiro());
        assertEquals("resposta do cliente", doBanco.getRespostaMsg());
    }

    @DisplayName("Não salva resposta sem mensagem")
    @Test
    void naoSalvaSemMensagem() {
        ChamadoEntity chamadoDoBanco = em.find(ChamadoEntity.class, chamado.getId());

        RespostaChamadoEntity semMensagem = RespostaChamadoEntity.builder()
                .chamado(chamadoDoBanco)
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semMensagem));
    }

    @DisplayName("Não salva resposta sem chamado")
    @Test
    void naoSalvaSemChamado() {
        RespostaChamadoEntity semChamado = RespostaChamadoEntity.builder()
                .respostaMsg("sem chamado")
                .build();

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(semChamado));
    }

    @DisplayName("Listar")
    @Test
    void listar() {
        List<RespostaChamadoEntity> lista = repository.findAll();
        List<String> mensagens = lista.stream().map(RespostaChamadoEntity::getRespostaMsg).toList();

        assertEquals(2, lista.size());
        assertTrue(mensagens.containsAll(List.of("primeira resposta", "segunda resposta")));
    }

    @DisplayName("Busca por Id")
    @Test
    void findById() {
        RespostaChamadoEntity doBanco = repository.findById(r1.getId()).orElseThrow();

        assertEquals(r1.getId(), doBanco.getId());
        assertEquals("primeira resposta", doBanco.getRespostaMsg());
        assertEquals(joao.getId(), doBanco.getBarbeiro().getId());
        assertEquals(chamado.getId(), doBanco.getChamado().getId());
    }

    @DisplayName("Busca por Id que não existe")
    @Test
    void findByIdInexistente() {
        assertTrue(repository.findById(9999L).isEmpty());
    }

    @DisplayName("Atualizar mensagem não altera a data de envio")
    @Test
    void atualizarNaoAlteraDataEnvio() {
        RespostaChamadoEntity resposta = repository.findById(r1.getId()).orElseThrow();
        var dataOriginal = resposta.getDataEnvio();

        resposta.setRespostaMsg("mensagem editada");
        repository.saveAndFlush(resposta);
        em.clear();

        RespostaChamadoEntity doBanco = repository.findById(r1.getId()).orElseThrow();

        assertEquals("mensagem editada", doBanco.getRespostaMsg());
        assertEquals(dataOriginal, doBanco.getDataEnvio());
    }

    @DisplayName("Deletar resposta")
    @Test
    void deletar() {
        repository.deleteById(r1.getId());
        em.flush();
        em.clear();

        assertTrue(repository.findById(r1.getId()).isEmpty());
        assertTrue(repository.findById(r2.getId()).isPresent());
    }
}