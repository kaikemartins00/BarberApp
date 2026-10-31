package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
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
        return UsuarioEntity.builder().id(id).email("user" + id + "@email.com").senha("123456").verificado(true)
                .roles(new HashSet<>(Set.of(RoleEntity.builder().name(role).build()))).build();
    }

    private void autenticar(UsuarioEntity usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private MensagemEntity mensagem(Long id, ConversaEntity conversa, UsuarioEntity remetente, String texto,
                                    LocalDateTime enviadaEm) {
        return MensagemEntity.builder().id(id).conversa(conversa).remetente(remetente).texto(texto)
                .enviadaEm(enviadaEm).lida(false).build();
    }

    @Test
    @DisplayName("Should list client conversations ordered by latest message, empty ones last")
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
    @DisplayName("Should list barber conversations showing the client name and unread count")
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
    @DisplayName("Should throw ForbiddenException when user is not a client on iniciar")
    void iniciarNotClientError() {

        autenticar(usuarioBarbeiro);

        when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> conversaService.iniciar(new ConversaRequest(2L)));
    }

    @Test
    @DisplayName("Should throw NotFoundException when barber does not exist on iniciar")
    void iniciarBarbeiroNotFoundError() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> conversaService.iniciar(new ConversaRequest(99L)));

        verify(conversaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create conversation when it does not exist")
    void iniciarNewSuccess() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(conversaRepository.findByClienteIdAndBarbeiroId(1L, 2L)).thenReturn(Optional.empty());
        when(conversaRepository.save(any(ConversaEntity.class))).thenReturn(conversa);

        ConversaResponse response = conversaService.iniciar(new ConversaRequest(2L));

        ArgumentCaptor<ConversaEntity> captor = ArgumentCaptor.forClass(ConversaEntity.class);

        verify(conversaRepository).save(captor.capture());

        assertSame(cliente, captor.getValue().getCliente());
        assertSame(barbeiro, captor.getValue().getBarbeiro());
        assertEquals("Joao", response.interlocutorNome());
        assertFalse(response.encerrada());
    }

    @Test
    @DisplayName("Should reuse and reopen an existing closed conversation")
    void iniciarReopensExistingSuccess() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(conversaRepository.findByClienteIdAndBarbeiroId(1L, 2L)).thenReturn(Optional.of(conversaEncerrada));

        ConversaResponse response = conversaService.iniciar(new ConversaRequest(2L));

        assertEquals(11L, response.id());
        assertFalse(conversaEncerrada.isEncerrada());
        assertFalse(response.encerrada());

        verify(conversaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when conversation does not exist on mensagens")
    void mensagensNotFoundError() {

        when(conversaRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> conversaService.mensagens(99L, null));

        assertThat(e.getMessage(), is("Conversa não encontrada"));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user is not a participant on mensagens")
    void mensagensNotParticipantError() {

        autenticar(usuarioEstranho);

        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> conversaService.mensagens(10L, null));

        assertThat(e.getMessage(), is("Você não participa dessa conversa."));

        verifyNoInteractions(mensagemRepository);
    }

    @Test
    @DisplayName("Should use 0 when depoisDe is null and flag own messages")
    void mensagensFromStartSuccess() {

        LocalDateTime agora = LocalDateTime.now();

        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.findByConversaIdAndIdGreaterThanOrderByIdAsc(10L, 0L)).thenReturn(List.of(
                mensagem(1L, conversa, usuarioCliente, "oi", agora),
                mensagem(2L, conversa, usuarioBarbeiro, "ola", agora)));

        List<MensagemResponse> lista = conversaService.mensagens(10L, null);

        assertEquals(2, lista.size());
        assertTrue(lista.get(0).minha());
        assertFalse(lista.get(1).minha());

        verify(mensagemRepository).findByConversaIdAndIdGreaterThanOrderByIdAsc(10L, 0L);
    }

    @Test
    @DisplayName("Should respect the given depoisDe id")
    void mensagensAfterIdSuccess() {

        autenticar(usuarioBarbeiro);

        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.findByConversaIdAndIdGreaterThanOrderByIdAsc(10L, 5L)).thenReturn(List.of());

        assertTrue(conversaService.mensagens(10L, 5L).isEmpty());
    }

    @Test
    @DisplayName("Should send message with trimmed text")
    void enviarSuccess() {

        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.save(any(MensagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        MensagemResponse response = conversaService.enviar(10L, new MensagemRequest("  oi, tudo bem?  "));

        ArgumentCaptor<MensagemEntity> captor = ArgumentCaptor.forClass(MensagemEntity.class);

        verify(mensagemRepository).save(captor.capture());

        assertEquals("oi, tudo bem?", captor.getValue().getTexto());
        assertSame(usuarioCliente, captor.getValue().getRemetente());
        assertSame(conversa, captor.getValue().getConversa());
        assertTrue(response.minha());
    }

    @Test
    @DisplayName("Should throw BadRequestException when sending to a closed conversation")
    void enviarClosedError() {

        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> conversaService.enviar(11L, new MensagemRequest("oi")));

        assertThat(e.getMessage(), is("Essa conversa foi finalizada."));

        verify(mensagemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non participant sends message")
    void enviarNotParticipantError() {

        autenticar(usuarioEstranho);

        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        assertThrows(ForbiddenException.class, () -> conversaService.enviar(10L, new MensagemRequest("oi")));

        verify(mensagemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark messages as read")
    void marcarComoLidasSuccess() {

        autenticar(usuarioBarbeiro);

        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        conversaService.marcarComoLidas(10L);

        verify(mensagemRepository).marcarComoLidas(10L, 2L);
    }

    @Test
    @DisplayName("Should throw ForbiddenException when non participant marks messages as read")
    void marcarComoLidasNotParticipantError() {

        autenticar(usuarioEstranho);

        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        assertThrows(ForbiddenException.class, () -> conversaService.marcarComoLidas(10L));

        verify(mensagemRepository, never()).marcarComoLidas(any(), any());
    }

    @Test
    @DisplayName("Should finish conversation")
    void finalizarSuccess() {

        autenticar(usuarioBarbeiro);

        when(conversaRepository.findById(10L)).thenReturn(Optional.of(conversa));

        ConversaResponse response = conversaService.finalizar(10L);

        assertTrue(conversa.isEncerrada());
        assertTrue(response.encerrada());
    }

    @Test
    @DisplayName("Should throw BadRequestException when conversation is already finished")
    void finalizarAlreadyFinishedError() {

        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        BadRequestException e = assertThrows(BadRequestException.class, () -> conversaService.finalizar(11L));

        assertThat(e.getMessage(), is("Essa conversa já foi finalizada."));
    }

    @Test
    @DisplayName("Should let the client reopen the conversation")
    void reabrirSuccess() {

        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        ConversaResponse response = conversaService.reabrir(11L);

        assertFalse(conversaEncerrada.isEncerrada());
        assertFalse(response.encerrada());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when barber tries to reopen")
    void reabrirBarbeiroError() {

        autenticar(usuarioBarbeiro);

        when(conversaRepository.findById(11L)).thenReturn(Optional.of(conversaEncerrada));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> conversaService.reabrir(11L));

        assertThat(e.getMessage(), is("Só o cliente pode reabrir a conversa."));
        assertTrue(conversaEncerrada.isEncerrada());
    }
}