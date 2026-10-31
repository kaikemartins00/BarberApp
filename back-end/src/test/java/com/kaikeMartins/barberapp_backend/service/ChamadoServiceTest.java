package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;
import com.kaikeMartins.barberapp_backend.dto.ChamadoRequest;
import com.kaikeMartins.barberapp_backend.dto.ChamadoResponse;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ChamadoRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.RespostaRepository;
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

import java.util.ArrayList;
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
public class ChamadoServiceTest {

    @InjectMocks
    private ChamadoService chamadoService;

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private RespostaRepository respostaRepository;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private ClienteRepository clienteRepository;

    private UsuarioEntity usuarioCliente;

    private UsuarioEntity usuarioBarbeiro;

    private UsuarioEntity usuarioOutroBarbeiro;

    private UsuarioEntity usuarioAdmin;

    private ClienteEntity cliente;

    private BarbeiroEntity barbeiro;

    private ChamadoEntity chamadoAberto;

    private ChamadoEntity chamadoEmAndamento;

    private ChamadoEntity chamadoDeOutro;

    private ChamadoEntity chamadoFinalizado;

    @BeforeEach
    void setUp() {
        usuarioCliente = usuario(1L, "ROLE_CLIENTE");
        usuarioBarbeiro = usuario(2L, "ROLE_BARBEIRO");
        usuarioOutroBarbeiro = usuario(3L, "ROLE_BARBEIRO");
        usuarioAdmin = usuario(5L, "ROLE_ADMIN");

        cliente = ClienteEntity.builder().id(1L).usuario(usuarioCliente).nome("Kaike").telefone("79999999999").build();
        barbeiro = BarbeiroEntity.builder().id(2L).usuario(usuarioBarbeiro).nome("Joao").telefone("79988888888").build();

        chamadoAberto = chamado(10L, ChamadoStatus.ABERTO, null);
        chamadoEmAndamento = chamado(11L, ChamadoStatus.EM_ANDAMENTO, usuarioBarbeiro);
        chamadoDeOutro = chamado(12L, ChamadoStatus.EM_ANDAMENTO, usuarioOutroBarbeiro);
        chamadoFinalizado = chamado(13L, ChamadoStatus.FINALIZADO, usuarioBarbeiro);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UsuarioEntity usuario(Long id, String role) {
        return UsuarioEntity.builder().id(id).email("user" + id + "@email.com").senha("123456").verificado(true)
                .roles(new HashSet<>(Set.of(RoleEntity.builder().name(role).build()))).build();
    }

    private ChamadoEntity chamado(Long id, ChamadoStatus status, UsuarioEntity responsavel) {
        return ChamadoEntity.builder().id(id).titulo("Titulo " + id).descricao("Descricao " + id)
                .status(status).prioridade(PrioridadeChamado.MEDIA).cliente(cliente).responsavel(responsavel)
                .respostas(new ArrayList<>()).build();
    }

    private void autenticar(UsuarioEntity usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    @Test
    @DisplayName("Should let admin see all tickets")
    void findAllAdminSuccess() {

        autenticar(usuarioAdmin);

        when(chamadoRepository.findAllByOrderByChamadoDateDesc())
                .thenReturn(List.of(chamadoAberto, chamadoEmAndamento, chamadoFinalizado));

        List<ChamadoResponse> lista = chamadoService.findAll();

        assertEquals(List.of(10L, 11L, 13L), lista.stream().map(ChamadoResponse::id).toList());
        assertTrue(lista.get(0).podeAtender());
        assertFalse(lista.get(1).podeAtender());
        assertFalse(lista.get(2).podeAtender());
    }

    @Test
    @DisplayName("Should let barber see free tickets and own tickets only")
    void findAllBarbeiroSuccess() {

        autenticar(usuarioBarbeiro);

        ChamadoEntity finalizadoDeOutro = chamado(14L, ChamadoStatus.FINALIZADO, usuarioOutroBarbeiro);
        ChamadoEntity finalizadoSemDono = chamado(15L, ChamadoStatus.FINALIZADO, null);

        when(chamadoRepository.findAllByOrderByChamadoDateDesc()).thenReturn(List.of(
                chamadoAberto, chamadoEmAndamento, chamadoDeOutro, chamadoFinalizado,
                finalizadoDeOutro, finalizadoSemDono));

        List<ChamadoResponse> lista = chamadoService.findAll();

        assertEquals(List.of(10L, 11L, 13L), lista.stream().map(ChamadoResponse::id).toList());
    }

    @Test
    @DisplayName("Should return tickets of the logged client")
    void meusSuccess() {

        autenticar(usuarioCliente);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(chamadoRepository.findByClienteIdOrderByChamadoDateDesc(1L)).thenReturn(List.of(chamadoAberto));

        List<ChamadoResponse> lista = chamadoService.meus();

        assertEquals(1, lista.size());
        assertEquals("Kaike", lista.get(0).clienteNome());
        assertEquals("user1@email.com", lista.get(0).clienteEmail());

        verify(chamadoRepository).findByClienteIdOrderByChamadoDateDesc(1L);
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user is not a client on meus")
    void meusNotClientError() {

        autenticar(usuarioBarbeiro);

        when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> chamadoService.meus());
    }

    @Test
    @DisplayName("Should create ticket as ABERTO for the logged client")
    void createSuccess() {

        autenticar(usuarioCliente);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(chamadoRepository.save(any(ChamadoEntity.class))).thenReturn(chamadoAberto);

        ChamadoResponse response = chamadoService.create(new ChamadoRequest("Duvida", "Como agendar?"));

        ArgumentCaptor<ChamadoEntity> captor = ArgumentCaptor.forClass(ChamadoEntity.class);

        verify(chamadoRepository).save(captor.capture());

        assertEquals(ChamadoStatus.ABERTO, captor.getValue().getStatus());
        assertEquals(PrioridadeChamado.BAIXA, captor.getValue().getPrioridade());
        assertSame(cliente, captor.getValue().getCliente());
        assertEquals(ChamadoStatus.ABERTO, response.status());
    }

    @Test
    @DisplayName("Should set ALTA priority when text mentions server")
    void createHighPrioritySuccess() {

        autenticar(usuarioCliente);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(chamadoRepository.save(any(ChamadoEntity.class))).thenReturn(chamadoAberto);

        chamadoService.create(new ChamadoRequest("O Servidor caiu", "ajuda"));

        ArgumentCaptor<ChamadoEntity> captor = ArgumentCaptor.forClass(ChamadoEntity.class);

        verify(chamadoRepository).save(captor.capture());

        assertEquals(PrioridadeChamado.ALTA, captor.getValue().getPrioridade());
    }

    @Test
    @DisplayName("Should set MEDIA priority when text mentions bug")
    void createMediumPrioritySuccess() {

        autenticar(usuarioCliente);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(chamadoRepository.save(any(ChamadoEntity.class))).thenReturn(chamadoAberto);

        chamadoService.create(new ChamadoRequest("Problema", "Encontrei um BUG na tela"));

        ArgumentCaptor<ChamadoEntity> captor = ArgumentCaptor.forClass(ChamadoEntity.class);

        verify(chamadoRepository).save(captor.capture());

        assertEquals(PrioridadeChamado.MEDIA, captor.getValue().getPrioridade());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user is not a client on create")
    void createNotClientError() {

        autenticar(usuarioBarbeiro);

        when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> chamadoService.create(new ChamadoRequest("t", "d")));

        verify(chamadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when assuming non-existent ticket")
    void assumirNotFoundError() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> chamadoService.assumir(99L));

        assertThat(e.getMessage(), is("Chamado não existe"));
    }

    @Test
    @DisplayName("Should throw BadRequestException when assuming finished ticket")
    void assumirFinishedError() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(13L)).thenReturn(Optional.of(chamadoFinalizado));

        BadRequestException e = assertThrows(BadRequestException.class, () -> chamadoService.assumir(13L));

        assertThat(e.getMessage(), is("Esse chamado já foi finalizado."));
    }

    @Test
    @DisplayName("Should assume free ticket and move it to EM_ANDAMENTO")
    void assumirSuccess() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(10L)).thenReturn(Optional.of(chamadoAberto));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        ChamadoResponse response = chamadoService.assumir(10L);

        assertSame(usuarioBarbeiro, chamadoAberto.getResponsavel());
        assertSame(barbeiro, chamadoAberto.getBarbeiro());
        assertEquals(ChamadoStatus.EM_ANDAMENTO, chamadoAberto.getStatus());
        assertEquals("Joao", response.responsavelNome());
        assertTrue(response.podeAtender());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when assuming ticket of another responsible")
    void assumirOtherResponsibleError() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(12L)).thenReturn(Optional.of(chamadoDeOutro));

        assertThrows(ForbiddenException.class, () -> chamadoService.assumir(12L));

        assertSame(usuarioOutroBarbeiro, chamadoDeOutro.getResponsavel());
    }

    @Test
    @DisplayName("Should let admin take ticket from another responsible")
    void assumirAdminSuccess() {

        autenticar(usuarioAdmin);

        when(chamadoRepository.buscarParaAtualizar(12L)).thenReturn(Optional.of(chamadoDeOutro));

        ChamadoResponse response = chamadoService.assumir(12L);

        assertSame(usuarioAdmin, chamadoDeOutro.getResponsavel());
        assertNull(chamadoDeOutro.getBarbeiro());
        assertEquals("Administração", response.responsavelNome());
    }

    @Test
    @DisplayName("Should answer ticket, assume it and return the barber name")
    void responderSuccess() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(10L)).thenReturn(Optional.of(chamadoAberto));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        ChamadoResponse response = chamadoService.responder(10L, new RespostaChamRequest("Vou verificar"));

        ArgumentCaptor<RespostaChamadoEntity> captor = ArgumentCaptor.forClass(RespostaChamadoEntity.class);

        verify(respostaRepository).save(captor.capture());

        assertEquals("Vou verificar", captor.getValue().getRespostaMsg());
        assertSame(barbeiro, captor.getValue().getBarbeiro());
        assertSame(chamadoAberto, captor.getValue().getChamado());
        assertEquals(ChamadoStatus.EM_ANDAMENTO, chamadoAberto.getStatus());
        assertEquals(1, response.respostas().size());
        assertEquals("Joao", response.respostas().get(0).autorNome());
    }

    @Test
    @DisplayName("Should show admin answer as Administração")
    void responderAdminSuccess() {

        autenticar(usuarioAdmin);

        when(chamadoRepository.buscarParaAtualizar(10L)).thenReturn(Optional.of(chamadoAberto));

        ChamadoResponse response = chamadoService.responder(10L, new RespostaChamRequest("Resolvido"));

        assertEquals("Administração", response.respostas().get(0).autorNome());
        assertNull(chamadoAberto.getRespostas().get(0).getBarbeiro());
    }

    @Test
    @DisplayName("Should throw BadRequestException when answering finished ticket")
    void responderFinishedError() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(13L)).thenReturn(Optional.of(chamadoFinalizado));

        assertThrows(BadRequestException.class,
                () -> chamadoService.responder(13L, new RespostaChamRequest("oi")));

        verify(respostaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when answering ticket of another responsible")
    void responderOtherResponsibleError() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(12L)).thenReturn(Optional.of(chamadoDeOutro));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> chamadoService.responder(12L, new RespostaChamRequest("oi")));

        assertThat(e.getMessage(), is("Esse chamado já está sendo atendido por Administração."));

        verify(respostaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should finish ticket")
    void finalizarSuccess() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(11L)).thenReturn(Optional.of(chamadoEmAndamento));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        ChamadoResponse response = chamadoService.finalizar(11L);

        assertEquals(ChamadoStatus.FINALIZADO, chamadoEmAndamento.getStatus());
        assertEquals(ChamadoStatus.FINALIZADO, response.status());
        assertFalse(response.podeAtender());
    }

    @Test
    @DisplayName("Should throw BadRequestException when finishing an already finished ticket")
    void finalizarAlreadyFinishedError() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(13L)).thenReturn(Optional.of(chamadoFinalizado));

        assertThrows(BadRequestException.class, () -> chamadoService.finalizar(13L));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when finishing ticket of another responsible")
    void finalizarOtherResponsibleError() {

        autenticar(usuarioBarbeiro);

        when(chamadoRepository.buscarParaAtualizar(12L)).thenReturn(Optional.of(chamadoDeOutro));

        assertThrows(ForbiddenException.class, () -> chamadoService.finalizar(12L));

        assertEquals(ChamadoStatus.EM_ANDAMENTO, chamadoDeOutro.getStatus());
    }
}