package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.ConversaRequest;
import com.kaikeMartins.barberapp_backend.dto.ConversaResponse;
import com.kaikeMartins.barberapp_backend.dto.MensagemRequest;
import com.kaikeMartins.barberapp_backend.dto.MensagemResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.ConversaRepository;
import com.kaikeMartins.barberapp_backend.repository.MensagemRepository;
import org.junit.jupiter.api.AfterEach;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConversaServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @InjectMocks
    private ConversaService conversaService;

    @Mock
    private ConversaRepository conversaRepository;

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    private UsuarioEntity usuarioCliente;

    private UsuarioEntity usuarioBarbeiro;

    private UsuarioEntity usuarioEstranho;

    private ClienteEntity cliente;

    private BarbeiroEntity barbeiro;

    private ConversaEntity conversa;

    private ConversaEntity conversaEncerrada;

    @BeforeEach
    void setUp() {
        usuarioCliente = usuario(1L, "ROLE_CLIENTE");
        usuarioBarbeiro = usuario(2L, "ROLE_BARBEIRO");
        usuarioEstranho = usuario(9L, "ROLE_CLIENTE");

        cliente = ClienteEntity.builder().id(1L).usuario(usuarioCliente).nome("Kaike").telefone("79999999999").build();
        barbeiro = BarbeiroEntity.builder().id(2L).usuario(usuarioBarbeiro).nome("Joao").telefone("79988888888").build();

        conversa = ConversaEntity.builder().id(10L).cliente(cliente).barbeiro(barbeiro).encerrada(false).build();
        conversaEncerrada = ConversaEntity.builder().id(11L).cliente(cliente).barbeiro(barbeiro).encerrada(true).build();

        autenticar(usuarioCliente);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioEntity usuario(Long id, String role) {
        return UsuarioEntity.builder()
                .id(id)
                .email("user" + id + "@email.com")
                .senha("123456")
                .verificado(true)
                .roles(new HashSet<>(Set.of(RoleEntity.builder().name(role).build())))
                .build();
    }

    private void autenticar(UsuarioEntity usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private AgendamentoEntity vinculo(AgendamentoStatus status) {
        return AgendamentoEntity.builder().cliente(cliente).barbeiro(barbeiro).status(status).build();
    }

    private MensagemEntity mensagem(Long id, ConversaEntity conversa, UsuarioEntity remetente, String texto,
                                    LocalDateTime enviadaEm) {
        return MensagemEntity.builder()
                .id(id)
                .conversa(conversa)
                .remetente(remetente)
                .texto(texto)
                .enviadaEm(enviadaEm)
                .lida(false)
                .build();
    }

    @Test
    @DisplayName("Deve listar as conversas do cliente da mais recente para a mais antiga, sem mensagens por último")
    void minhasConversasClienteSuccess() {
        ConversaEntity c2 = ConversaEntity.builder().id(2L).cliente(cliente).barbeiro(barbeiro).build();
        ConversaEntity c3 = ConversaEntity.builder().id(3L).cliente(cliente).barbeiro(barbeiro).build();
        LocalDateTime agora = LocalDateTime.now();
        when(conversaRepository.findByClienteId(1L)).thenReturn(List.of(conversa, c2, c3));
        when(mensagemRepository.findFirstByConversaIdOrderByIdDesc(10L))
                .thenReturn(Optional.of(mensagem(100L, conversa, usuarioCliente, "antiga", agora.minusHours(2))));
        when(mensagemRepository.findFirstByConversaIdOrderByIdDesc(2L))
                .thenReturn(Optional.of(mensagem(101L, c2, usuarioBarbeiro, "recente", agora.minusMinutes(5))));
        when(mensagemRepository.findFirstByConversaIdOrderByIdDesc(3L)).thenReturn(Optional.empty());

        List<ConversaResponse> lista = conversaService.minhasConversas();

        assertEquals(List.of(2L, 10L, 3L), lista.stream().map(ConversaResponse::id).toList());
        assertEquals("Joao", lista.get(0).interlocutorNome());
        assertEquals("recente", lista.get(0).ultimaMensagem());
        assertNull(lista.get(2).ultimaMensagem());
        assertNull(lista.get(2).ultimaMensagemEm());
        verify(conversaRepository, never()).findByBarbeiroId(any());
    }

    @Test
    @DisplayName("Deve listar as conversas do barbeiro mostrando o nome do cliente e as não lidas")
    void minhasConversasBarbeiroSuccess() {
        autenticar(usuarioBarbeiro);
        when(conversaRepository.findByBarbeiroId(2L)).thenReturn(List.of(conversa));
        when(mensagemRepository.findFirstByConversaIdOrderByIdDesc(10L)).thenReturn(Optional.empty());
        when(mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(10L, 2L)).thenReturn(3L);

        List<ConversaResponse> lista = conversaService.minhasConversas();

        assertEquals(1, lista.size());
        assertEquals("Kaike", lista.get(0).interlocutorNome());
        assertEquals(3L, lista.get(0).naoLidas());
        verify(conversaRepository, never()).findByClienteId(any());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o cliente não tem conversas")
    void minhasConversasEmptySuccess() {
        when(conversaRepository.findByClienteId(1L)).thenReturn(List.of());

        List<ConversaResponse> lista = conversaService.minhasConversas();

        assertTrue(lista.isEmpty());
        verifyNoInteractions(mensagemRepository);
    }

    @Test
    @DisplayName("Deve informar o status encerrada e a última mensagem na lista de conversas")
    void minhasConversasLastMessageSuccess() {
        LocalDateTime enviada = LocalDateTime.now().minusMinutes(1);
        when(conversaRepository.findByClienteId(1L)).thenReturn(List.of(conversaEncerrada));
        when(mensagemRepository.findFirstByConversaIdOrderByIdDesc(11L))
                .thenReturn(Optional.of(mensagem(100L, conversaEncerrada, usuarioBarbeiro, "tchau", enviada)));
        when(mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(11L, 1L)).thenReturn(2L);

        List<ConversaResponse> lista = conversaService.minhasConversas();

        assertTrue(lista.get(0).encerrada());
        assertEquals("tchau", lista.get(0).ultimaMensagem());
        assertEquals(enviada, lista.get(0).ultimaMensagemEm());
        assertEquals(2L, lista.get(0).naoLidas());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException na listagem quando o principal não for um usuário")
    void minhasConversasUnauthenticatedError() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThrows(ForbiddenException.class, () -> conversaService.minhasConversas());

        verifyNoInteractions(conversaRepository, mensagemRepository);
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o perfil não é cliente nem barbeiro em iniciar")
    void iniciarNotClientError() {
        autenticar(usuario(3L, "ROLE_ADMIN"));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> conversaService.iniciar(new ConversaRequest(2L, 1L)));

        assertThat(e.getMessage(), is("Perfil sem permissão para iniciar conversas."));
        verify(conversaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o cliente não informa o barbeiro")
    void iniciarClienteSemBarbeiroError() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> conversaService.iniciar(new ConversaRequest(null, null)));

        assertThat(e.getMessage(), is("Informe o barbeiro."));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o cliente não tem horário com o barbeiro")
    void iniciarClienteSemVinculoError() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByClienteId(1L)).thenReturn(List.of(vinculo(AgendamentoStatus.CANCELADO)));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> conversaService.iniciar(new ConversaRequest(2L, null)));

        assertThat(e.getMessage(), is("Você só pode conversar com barbeiros com quem tem horário agendado."));
        verify(conversaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o barbeiro não informa o cliente")
    void iniciarBarbeiroSemClienteError() {
        autenticar(usuarioBarbeiro);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> conversaService.iniciar(new ConversaRequest(null, null)));

        assertThat(e.getMessage(), is("Informe o cliente."));
    }

    @Test
    @DisplayName("Deve iniciar a conversa pelo barbeiro quando o cliente é dos seus atendimentos")
    void iniciarPeloBarbeiroSuccess() {
        autenticar(usuarioBarbeiro);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroId(2L)).thenReturn(List.of(vinculo(AgendamentoStatus.PENDENTE)));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(conversaRepository.findByClienteIdAndBarbeiroId(1L, 2L)).thenReturn(Optional.of(conversa));

        ConversaResponse response = conversaService.iniciar(new ConversaRequest(null, 1L));

        assertEquals(10L, response.id());
        verify(conversaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o barbeiro não atendeu o cliente")
    void iniciarPeloBarbeiroSemVinculoError() {
        autenticar(usuarioBarbeiro);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroId(2L)).thenReturn(List.of());

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> conversaService.iniciar(new ConversaRequest(null, 1L)));

        assertThat(e.getMessage(), is("Você só pode conversar com clientes dos seus atendimentos."));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o barbeiro não existir em iniciar")
    void iniciarBarbeiroNotFoundError() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> conversaService.iniciar(new ConversaRequest(99L, null)));

        assertThat(e.getMessage(), is("Barbeiro não encontrado"));
        verify(conversaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar a conversa quando ela não existir")
    void iniciarNewSuccess() {
        when(agendamentoRepository.findByClienteId(1L)).thenReturn(List.of(vinculo(AgendamentoStatus.CONFIRMADO)));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(conversaRepository.findByClienteIdAndBarbeiroId(1L, 2L)).thenReturn(Optional.empty());
        when(conversaRepository.save(any(ConversaEntity.class))).thenReturn(conversa);

        ConversaResponse response = conversaService.iniciar(new ConversaRequest(2L, null));

        ArgumentCaptor<ConversaEntity> captor = ArgumentCaptor.forClass(ConversaEntity.class);
        verify(conversaRepository).save(captor.capture());

        assertSame(cliente, captor.getValue().getCliente());
        assertSame(barbeiro, captor.getValue().getBarbeiro());
        assertEquals("Joao", response.interlocutorNome());
        assertFalse(response.encerrada());
    }

    @Test
    @DisplayName("Deve reaproveitar a conversa existente que está aberta")
    void iniciarExistingOpenSuccess() {
        when(agendamentoRepository.findByClienteId(1L)).thenReturn(List.of(vinculo(AgendamentoStatus.CONFIRMADO)));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(conversaRepository.findByClienteIdAndBarbeiroId(1L, 2L)).thenReturn(Optional.of(conversa));

        ConversaResponse response = conversaService.iniciar(new ConversaRequest(2L, null));

        assertEquals(10L, response.id());
        assertFalse(response.encerrada());
        verify(conversaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve reaproveitar e reabrir uma conversa encerrada")
    void iniciarReopensExistingSuccess() {
        when(agendamentoRepository.findByClienteId(1L)).thenReturn(List.of(vinculo(AgendamentoStatus.CONFIRMADO)));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(conversaRepository.findByClienteIdAndBarbeiroId(1L, 2L)).thenReturn(Optional.of(conversaEncerrada));

        ConversaResponse response = conversaService.iniciar(new ConversaRequest(2L, null));

        assertEquals(11L, response.id());
        assertFalse(conversaEncerrada.isEncerrada());
        assertFalse(response.encerrada());
        verify(conversaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a conversa não existir em mensagens")
    void mensagensNotFoundError() {
        when(conversaRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> conversaService.mensagens(99L, null));

        assertThat(e.getMessage(), is("Conversa não encontrada"));
        verifyNoInteractions(mensagemRepository);
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o usuário não participa da conversa em mensagens")
    void mensagensNotParticipantError() {
        autenticar(usuarioEstranho);
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> conversaService.mensagens(10L, null));

        assertThat(e.getMessage(), is("Você não participa dessa conversa."));
        verifyNoInteractions(mensagemRepository);
    }

    @Test
    @DisplayName("Deve usar 0 quando depoisDe for nulo e marcar as mensagens que são minhas")
    void mensagensFromStartSuccess() {
        LocalDateTime agora = LocalDateTime.now();
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.findByConversaIdAndIdGreaterThanOrderByIdAsc(10L, 0L)).thenReturn(List.of(
                mensagem(1L, conversa, usuarioCliente, "oi", agora),
                mensagem(2L, conversa, usuarioBarbeiro, "ola", agora)));

        List<MensagemResponse> lista = conversaService.mensagens(10L, null);

        assertEquals(2, lista.size());
        assertEquals("oi", lista.get(0).texto());
        assertEquals(agora, lista.get(0).enviadaEm());
        assertTrue(lista.get(0).minha());
        assertFalse(lista.get(1).minha());
        verify(mensagemRepository).findByConversaIdAndIdGreaterThanOrderByIdAsc(10L, 0L);
    }

    @Test
    @DisplayName("Deve respeitar o id informado em depoisDe")
    void mensagensAfterIdSuccess() {
        autenticar(usuarioBarbeiro);
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.findByConversaIdAndIdGreaterThanOrderByIdAsc(10L, 5L)).thenReturn(List.of());

        List<MensagemResponse> lista = conversaService.mensagens(10L, 5L);

        assertTrue(lista.isEmpty());
        verify(mensagemRepository).findByConversaIdAndIdGreaterThanOrderByIdAsc(10L, 5L);
    }

    @Test
    @DisplayName("Deve marcar como minha apenas a mensagem enviada pelo barbeiro quando ele consulta")
    void mensagensBarbeiroOwnMessageSuccess() {
        autenticar(usuarioBarbeiro);
        LocalDateTime agora = LocalDateTime.now();
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.findByConversaIdAndIdGreaterThanOrderByIdAsc(10L, 0L)).thenReturn(List.of(
                mensagem(1L, conversa, usuarioCliente, "oi", agora),
                mensagem(2L, conversa, usuarioBarbeiro, "ola", agora)));

        List<MensagemResponse> lista = conversaService.mensagens(10L, null);

        assertFalse(lista.get(0).minha());
        assertTrue(lista.get(1).minha());
    }

    @Test
    @DisplayName("Deve enviar a mensagem com o texto sem espaços nas pontas")
    void enviarSuccess() {
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.save(any(MensagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        MensagemResponse response = conversaService.enviar(10L, new MensagemRequest("  oi, tudo bem?  "));

        ArgumentCaptor<MensagemEntity> captor = ArgumentCaptor.forClass(MensagemEntity.class);
        verify(mensagemRepository).save(captor.capture());

        assertEquals("oi, tudo bem?", captor.getValue().getTexto());
        assertSame(usuarioCliente, captor.getValue().getRemetente());
        assertSame(conversa, captor.getValue().getConversa());
        assertEquals("oi, tudo bem?", response.texto());
        assertTrue(response.minha());
    }

    @Test
    @DisplayName("Deve permitir que o barbeiro envie mensagem")
    void enviarBarbeiroSuccess() {
        autenticar(usuarioBarbeiro);
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.save(any(MensagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        MensagemResponse response = conversaService.enviar(10L, new MensagemRequest("ola"));

        ArgumentCaptor<MensagemEntity> captor = ArgumentCaptor.forClass(MensagemEntity.class);
        verify(mensagemRepository).save(captor.capture());

        assertSame(usuarioBarbeiro, captor.getValue().getRemetente());
        assertTrue(response.minha());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao enviar mensagem para conversa inexistente")
    void enviarNotFoundError() {
        when(conversaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> conversaService.enviar(99L, new MensagemRequest("oi")));

        verify(mensagemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao enviar para uma conversa encerrada")
    void enviarClosedError() {
        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> conversaService.enviar(11L, new MensagemRequest("oi")));

        assertThat(e.getMessage(), is("Essa conversa foi finalizada."));
        verify(mensagemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando quem não participa envia mensagem")
    void enviarNotParticipantError() {
        autenticar(usuarioEstranho);
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        assertThrows(ForbiddenException.class, () -> conversaService.enviar(10L, new MensagemRequest("oi")));

        verify(mensagemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve marcar as mensagens como lidas para o usuário logado")
    void marcarComoLidasSuccess() {
        autenticar(usuarioBarbeiro);
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        conversaService.marcarComoLidas(10L);

        verify(mensagemRepository).marcarComoLidas(10L, 2L);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao marcar como lidas uma conversa inexistente")
    void marcarComoLidasNotFoundError() {
        when(conversaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> conversaService.marcarComoLidas(99L));

        verifyNoInteractions(mensagemRepository);
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando quem não participa marca como lidas")
    void marcarComoLidasNotParticipantError() {
        autenticar(usuarioEstranho);
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        assertThrows(ForbiddenException.class, () -> conversaService.marcarComoLidas(10L));

        verify(mensagemRepository, never()).marcarComoLidas(any(), any());
    }

    @Test
    @DisplayName("Deve permitir que o barbeiro finalize a conversa")
    void finalizarBarbeiroSuccess() {
        autenticar(usuarioBarbeiro);
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        ConversaResponse response = conversaService.finalizar(10L);

        assertTrue(conversa.isEncerrada());
        assertTrue(response.encerrada());
        assertEquals("Kaike", response.interlocutorNome());
    }

    @Test
    @DisplayName("Deve permitir que o cliente finalize a conversa")
    void finalizarClienteSuccess() {
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        ConversaResponse response = conversaService.finalizar(10L);

        assertTrue(conversa.isEncerrada());
        assertTrue(response.encerrada());
        assertEquals("Joao", response.interlocutorNome());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando a conversa já estiver finalizada")
    void finalizarAlreadyFinishedError() {
        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        BadRequestException e = assertThrows(BadRequestException.class, () -> conversaService.finalizar(11L));

        assertThat(e.getMessage(), is("Essa conversa já foi finalizada."));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando quem não participa finaliza a conversa")
    void finalizarNotParticipantError() {
        autenticar(usuarioEstranho);
        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        assertThrows(ForbiddenException.class, () -> conversaService.finalizar(10L));

        assertFalse(conversa.isEncerrada());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao finalizar uma conversa inexistente")
    void finalizarNotFoundError() {
        when(conversaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> conversaService.finalizar(99L));
    }

    @Test
    @DisplayName("Deve permitir que o cliente reabra a conversa")
    void reabrirSuccess() {
        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        ConversaResponse response = conversaService.reabrir(11L);

        assertFalse(conversaEncerrada.isEncerrada());
        assertFalse(response.encerrada());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o barbeiro tenta reabrir")
    void reabrirBarbeiroError() {
        autenticar(usuarioBarbeiro);
        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> conversaService.reabrir(11L));

        assertThat(e.getMessage(), is("Só o cliente pode reabrir a conversa."));
        assertTrue(conversaEncerrada.isEncerrada());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando quem não participa tenta reabrir")
    void reabrirNotParticipantError() {
        autenticar(usuarioEstranho);
        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> conversaService.reabrir(11L));

        assertThat(e.getMessage(), is("Você não participa dessa conversa."));
        assertTrue(conversaEncerrada.isEncerrada());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao reabrir uma conversa inexistente")
    void reabrirNotFoundError() {
        when(conversaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> conversaService.reabrir(99L));
    }
}