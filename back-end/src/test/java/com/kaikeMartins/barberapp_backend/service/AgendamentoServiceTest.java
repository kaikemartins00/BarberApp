package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
public class AgendamentoServiceTest {

    @InjectMocks
    private AgendamentoService agendamentoService;

    @Mock
    private AgendamentoRepository repository;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    private UsuarioEntity usuarioCliente;

    private UsuarioEntity usuarioBarbeiro;

    private ClienteEntity cliente;

    private BarbeiroEntity barbeiro;

    private ServicoEntity servico;

    private AgendamentoEntity agendamento;

    private AgendamentoRequest request;

    private LocalDate amanha;

    private LocalTime hora;

    @BeforeEach
    void setUp() {
        amanha = LocalDate.now().plusDays(1);
        hora = LocalTime.of(14, 0);

        usuarioCliente = UsuarioEntity.builder()
                .id(1L)
                .email("kaike@email.com")
                .senha("123456")
                .verificado(true)
                .roles(Set.of(RoleEntity.builder()
                        .name("ROLE_CLIENTE")
                        .build()))
                .build();

        usuarioBarbeiro = UsuarioEntity.builder()
                .id(2L)
                .email("joao@email.com")
                .senha("123456")
                .verificado(true)
                .roles(Set.of(RoleEntity.builder()
                        .name("ROLE_BARBEIRO")
                        .build()))
                .build();

        cliente = ClienteEntity.builder()
                .id(1L)
                .usuario(usuarioCliente)
                .nome("Kaike")
                .telefone("79999999999")
                .build();

        servico = ServicoEntity.builder()
                .id(3L)
                .nome("Corte")
                .preco(40.0)
                .build();

        barbeiro = BarbeiroEntity.builder()
                .id(2L)
                .usuario(usuarioBarbeiro)
                .nome("Joao")
                .telefone("79988888888")
                .servicos(new HashSet<>(Set.of(servico)))
                .horariosTrabalho(new HashSet<>(Set.of(LocalDateTime.of(amanha, hora))))
                .build();

        agendamento = AgendamentoEntity.builder()
                .id(10L)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(amanha)
                .horario(hora)
                .status(AgendamentoStatus.PENDENTE).build();

        request = new AgendamentoRequest(2L
                , 3L
                , amanha
                , hora);

        autenticar(usuarioCliente);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(UsuarioEntity usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private UsuarioEntity usuarioComRole(Long id, String role) {
        return UsuarioEntity.builder().id(id).email("user" + id + "@email.com").senha("123456").verificado(true)
                .roles(Set.of(RoleEntity.builder().name(role).build())).build();
    }

    @Test
    @DisplayName("Should create appointment with PENDENTE status")
    void createSuccess() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));
        when(repository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(2L, amanha, hora, AgendamentoStatus.CANCELADO))
                .thenReturn(false);
        when(repository.save(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.create(request);

        assertEquals("Kaike", response.clienteNome());
        assertEquals("Joao", response.barbeiroNome());
        assertEquals("Corte", response.servicoNome());
        assertEquals(40.0, response.servicoPreco());
        assertEquals(amanha, response.data());
        assertEquals(hora, response.horario());
        assertEquals("PENDENTE", response.status());

        verify(repository).save(any(AgendamentoEntity.class));
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user is not a client")
    void createNotClientError() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> agendamentoService.create(request));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when principal is not a user")
    void createUnauthenticatedError() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThrows(ForbiddenException.class, () -> agendamentoService.create(request));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when barber does not exist")
    void createBarbeiroNotFoundError() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("Barbeiro não encontrado"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when service does not exist")
    void createServicoNotFoundError() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("Serviço não encontrado"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when barber does not perform the service")
    void createBarberDoesNotPerformServiceError() {

        barbeiro.getServicos().clear();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));

        BadRequestException e = assertThrows(BadRequestException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("Esse barbeiro não realiza esse serviço."));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when time is not in barber schedule")
    void createOutsideScheduleError() {

        barbeiro.getHorariosTrabalho().clear();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));

        BadRequestException e = assertThrows(BadRequestException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("O barbeiro não atende nesse dia e horário."));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when time is in the past")
    void createPastTimeError() {

        LocalDate ontem = LocalDate.now().minusDays(1);
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(ontem, hora));

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.create(new AgendamentoRequest(2L, 3L, ontem, hora)));

        assertThat(e.getMessage(), is("Esse horário já passou."));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when time is already taken")
    void createTimeTakenError() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));
        when(repository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(2L, amanha, hora, AgendamentoStatus.CANCELADO))
                .thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("Esse horário já está ocupado."));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should return free times sorted, ignoring taken, cancelled and other days")
    void horariosLivresSuccess() {

        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(11, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(10, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(9, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha.plusDays(1), LocalTime.of(9, 0)));

        AgendamentoEntity pendente = AgendamentoEntity.builder().id(11L).cliente(cliente).barbeiro(barbeiro)
                .servico(servico).data(amanha).horario(LocalTime.of(10, 0)).status(AgendamentoStatus.PENDENTE).build();
        AgendamentoEntity cancelado = AgendamentoEntity.builder().id(12L).cliente(cliente).barbeiro(barbeiro)
                .servico(servico).data(amanha).horario(LocalTime.of(11, 0)).status(AgendamentoStatus.CANCELADO).build();

        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(repository.findByBarbeiroIdAndData(2L, amanha)).thenReturn(List.of(pendente, cancelado));

        List<LocalTime> livres = agendamentoService.horariosLivres(2L, amanha);

        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(14, 0)), livres);
    }

    @Test
    @DisplayName("Should not return free times that already passed")
    void horariosLivresPastError() {

        LocalDate ontem = LocalDate.now().minusDays(1);
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(ontem, LocalTime.of(9, 0)));

        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(repository.findByBarbeiroIdAndData(2L, ontem)).thenReturn(List.of());

        assertTrue(agendamentoService.horariosLivres(2L, ontem).isEmpty());
    }

    @Test
    @DisplayName("Should throw NotFoundException when barber does not exist on free times")
    void horariosLivresNotFoundError() {

        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> agendamentoService.horariosLivres(99L, amanha));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return only the logged client appointments")
    void meusAgendamentosSuccess() {

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.findByClienteId(1L)).thenReturn(List.of(agendamento));

        List<AgendamentoResponse> lista = agendamentoService.meusAgendamentos();

        assertEquals(1, lista.size());
        assertEquals(10L, lista.get(0).id());
        assertEquals("Kaike", lista.get(0).clienteNome());

        verify(repository).findByClienteId(1L);
    }

    @Test
    @DisplayName("Should return only barber appointments when user is barber")
    void findAllBarbeiroSuccess() {

        autenticar(usuarioBarbeiro);

        when(repository.findByBarbeiroId(2L)).thenReturn(List.of(agendamento));

        List<AgendamentoResponse> lista = agendamentoService.findAll();

        assertEquals(1, lista.size());

        verify(repository).findByBarbeiroId(2L);
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("Should return all appointments when user is not barber")
    void findAllAdminSuccess() {

        autenticar(usuarioComRole(5L, "ROLE_ADMIN"));

        when(repository.findAll()).thenReturn(List.of(agendamento, agendamento));

        List<AgendamentoResponse> lista = agendamentoService.findAll();

        assertEquals(2, lista.size());

        verify(repository).findAll();
        verify(repository, never()).findByBarbeiroId(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating status of non-existent appointment")
    void atualizarStatusNotFoundError() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> agendamentoService.atualizarStatus(99L, AgendamentoStatus.CONFIRMADO));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when appointment is already cancelled")
    void atualizarStatusAlreadyCancelledError() {

        agendamento.setStatus(AgendamentoStatus.CANCELADO);

        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO));

        assertThat(e.getMessage(), is("Esse agendamento já foi cancelado."));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should let the owner barber confirm the appointment")
    void atualizarStatusBarbeiroSuccess() {

        autenticar(usuarioBarbeiro);

        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.save(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO);

        assertEquals("CONFIRMADO", response.status());
        assertEquals(AgendamentoStatus.CONFIRMADO, agendamento.getStatus());

        verify(repository).save(agendamento);
    }

    @Test
    @DisplayName("Should throw ForbiddenException when barber changes another barber appointment")
    void atualizarStatusOtherBarberError() {

        autenticar(usuarioComRole(7L, "ROLE_BARBEIRO"));

        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO));

        assertThat(e.getMessage(), is("Esse agendamento é de outro barbeiro."));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should let admin change any appointment")
    void atualizarStatusAdminSuccess() {

        autenticar(usuarioComRole(5L, "ROLE_ADMIN"));

        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.save(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO);

        assertEquals("CONFIRMADO", response.status());

        verify(repository).save(agendamento);
    }

    @Test
    @DisplayName("Should let client cancel own appointment")
    void atualizarStatusClientCancelSuccess() {

        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.save(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO);

        assertEquals("CANCELADO", response.status());

        verify(repository).save(agendamento);
    }

    @Test
    @DisplayName("Should throw ForbiddenException when client tries to confirm")
    void atualizarStatusClientConfirmError() {

        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO));

        assertThat(e.getMessage(), is("Clientes só podem cancelar agendamentos."));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when client cancels another client appointment")
    void atualizarStatusOtherClientError() {

        ClienteEntity outro = ClienteEntity.builder().id(9L).usuario(usuarioComRole(9L, "ROLE_CLIENTE"))
                .nome("Outro").telefone("79911111111").build();
        agendamento.setCliente(outro);

        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO));

        assertThat(e.getMessage(), is("Esse agendamento não é seu."));

        verify(repository, never()).save(any());
    }
}