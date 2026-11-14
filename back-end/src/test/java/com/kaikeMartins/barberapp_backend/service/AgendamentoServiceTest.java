package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.ResumoCancelamentosResponse;
import com.kaikeMartins.barberapp_backend.event.AvaliacaoSolicitadaEvent;
import com.kaikeMartins.barberapp_backend.event.ComprovanteCancelamentoEvent;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PagamentoService pagamentoService;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private AtendimentoServicoService atendimentoServicoService;

    @Mock
    private FilaEsperaService filaEsperaService;

    private UsuarioEntity usuarioCliente;

    private UsuarioEntity usuarioBarbeiro;

    private ClienteEntity cliente;

    private BarbeiroEntity barbeiro;

    private ServicoEntity servico;

    private AgendamentoEntity agendamento;

    private AgendamentoRequest request;

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

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
                .roles(Set.of(RoleEntity.builder().name("ROLE_CLIENTE").build()))
                .build();

        usuarioBarbeiro = UsuarioEntity.builder()
                .id(2L)
                .email("joao@email.com")
                .senha("123456")
                .verificado(true)
                .roles(Set.of(RoleEntity.builder().name("ROLE_BARBEIRO").build()))
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
                .status(AgendamentoStatus.PENDENTE)
                .build();

        request = new AgendamentoRequest(2L, 3L, amanha, hora);

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

    private void autenticarPrincipalInvalido() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
    }

    private UsuarioEntity usuarioComRole(Long id, String role) {
        return UsuarioEntity.builder()
                .id(id)
                .email("user" + id + "@email.com")
                .senha("123456")
                .verificado(true)
                .roles(Set.of(RoleEntity.builder().name(role).build()))
                .build();
    }

    private AgendamentoEntity agendamentoNoHorario(Long id, LocalTime horario, AgendamentoStatus status) {
        return AgendamentoEntity.builder()
                .id(id)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(amanha)
                .horario(horario)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("Deve criar agendamento com status PENDENTE")
    void createSuccess() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));
        when(repository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(2L, amanha, hora, AgendamentoStatus.CANCELADO))
                .thenReturn(false);
        when(repository.save(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.create(request);

        ArgumentCaptor<AgendamentoEntity> captor = ArgumentCaptor.forClass(AgendamentoEntity.class);
        verify(repository).save(captor.capture());

        assertSame(cliente, captor.getValue().getCliente());
        assertSame(barbeiro, captor.getValue().getBarbeiro());
        assertSame(servico, captor.getValue().getServico());
        assertEquals(amanha, captor.getValue().getData());
        assertEquals(hora, captor.getValue().getHorario());
        assertEquals(AgendamentoStatus.PENDENTE, captor.getValue().getStatus());
        assertEquals("Kaike", response.clienteNome());
        assertEquals("Joao", response.barbeiroNome());
        assertEquals("Corte", response.servicoNome());
        assertEquals(40.0, response.servicoPreco());
        assertEquals(amanha, response.data());
        assertEquals(hora, response.horario());
        assertEquals("PENDENTE", response.status());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o usuário não for um cliente")
    void createNotClientError() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("Apenas clientes podem usar esta função."));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o principal não for um usuário")
    void createUnauthenticatedError() {
        autenticarPrincipalInvalido();

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("Usuário não autenticado ou sessão inválida."));
        verifyNoInteractions(clienteRepository, barbeiroRepository, servicoRepository);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o barbeiro não existir")
    void createBarbeiroNotFoundError() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("Barbeiro não encontrado."));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o serviço não existir")
    void createServicoNotFoundError() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> agendamentoService.create(request));

        assertThat(e.getMessage(), is("Serviço não encontrado."));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o barbeiro não realiza o serviço")
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
    @DisplayName("Deve lançar BadRequestException quando o barbeiro não tem horários na agenda")
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
    @DisplayName("Deve lançar BadRequestException quando o horário pedido é diferente dos da agenda no mesmo dia")
    void createDifferentHourError() {
        AgendamentoRequest outroHorario = new AgendamentoRequest(2L, 3L, amanha, LocalTime.of(15, 0));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findById(3L)).thenReturn(Optional.of(servico));

        BadRequestException e = assertThrows(BadRequestException.class, () -> agendamentoService.create(outroHorario));

        assertThat(e.getMessage(), is("O barbeiro não atende nesse dia e horário."));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o horário for no passado")
    void createPastTimeError() {
        LocalDate ontem = LocalDate.now().minusDays(1);
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(ontem, hora));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.create(new AgendamentoRequest(2L, 3L, ontem, hora)));

        assertThat(e.getMessage(), is("Não é possível agendar um horário que já passou."));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o horário já estiver ocupado")
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
    @DisplayName("Deve retornar os horários livres ordenados, ignorando ocupados, cancelados e outros dias")
    void horariosLivresSuccess() {
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(11, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(10, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(9, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha.plusDays(1), LocalTime.of(9, 0)));
        AgendamentoEntity pendente = agendamentoNoHorario(11L, LocalTime.of(10, 0), AgendamentoStatus.PENDENTE);
        AgendamentoEntity cancelado = agendamentoNoHorario(12L, LocalTime.of(11, 0), AgendamentoStatus.CANCELADO);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(repository.findByBarbeiroIdAndData(2L, amanha)).thenReturn(List.of(pendente, cancelado));

        List<LocalTime> livres = agendamentoService.horariosLivres(2L, amanha);

        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(14, 0)), livres);
        verify(repository).findByBarbeiroIdAndData(2L, amanha);
    }

    @Test
    @DisplayName("Deve retornar todos os horários da agenda quando não há agendamentos no dia")
    void horariosLivresWithoutAppointmentsSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(repository.findByBarbeiroIdAndData(2L, amanha)).thenReturn(List.of());

        List<LocalTime> livres = agendamentoService.horariosLivres(2L, amanha);

        assertEquals(List.of(hora), livres);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando todos os horários do dia estão ocupados")
    void horariosLivresAllTakenSuccess() {
        AgendamentoEntity ocupado = agendamentoNoHorario(11L, hora, AgendamentoStatus.CONFIRMADO);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(repository.findByBarbeiroIdAndData(2L, amanha)).thenReturn(List.of(ocupado));

        List<LocalTime> livres = agendamentoService.horariosLivres(2L, amanha);

        assertTrue(livres.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o barbeiro não atende no dia")
    void horariosLivresDayWithoutScheduleSuccess() {
        LocalDate semAgenda = amanha.plusDays(10);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(repository.findByBarbeiroIdAndData(2L, semAgenda)).thenReturn(List.of());

        List<LocalTime> livres = agendamentoService.horariosLivres(2L, semAgenda);

        assertTrue(livres.isEmpty());
    }

    @Test
    @DisplayName("Não deve retornar horários livres de dias que já passaram")
    void horariosLivresPastDayError() {
        LocalDate ontem = LocalDate.now().minusDays(1);
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(ontem, LocalTime.of(9, 0)));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(repository.findByBarbeiroIdAndData(2L, ontem)).thenReturn(List.of());

        List<LocalTime> livres = agendamentoService.horariosLivres(2L, ontem);

        assertTrue(livres.isEmpty());
    }

    @Test
    @DisplayName("Não deve retornar horário de hoje que já passou")
    void horariosLivresPastHourTodayError() {
        LocalDate hoje = LocalDate.now();
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(hoje, LocalTime.MIN));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(repository.findByBarbeiroIdAndData(2L, hoje)).thenReturn(List.of());

        List<LocalTime> livres = agendamentoService.horariosLivres(2L, hoje);

        assertTrue(livres.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o barbeiro não existir nos horários livres")
    void horariosLivresNotFoundError() {
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> agendamentoService.horariosLivres(99L, amanha));

        assertThat(e.getMessage(), is("Barbeiro não encontrado."));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Deve retornar apenas os agendamentos do cliente logado")
    void meusAgendamentosSuccess() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.findByClienteId(1L)).thenReturn(List.of(agendamento));
        when(pagamentoRepository.findByAgendamentoId(agendamento.getId())).thenReturn(Optional.of(
                PagamentoEntity.builder()
                        .metodo(PagamentoMetodo.PRESENCIAL)
                        .status(PagamentoStatus.PENDENTE)
                        .build()));

        List<AgendamentoResponse> lista = agendamentoService.meusAgendamentos();

        assertEquals(1, lista.size());
        assertEquals(10L, lista.get(0).id());
        assertEquals("Kaike", lista.get(0).clienteNome());
        verify(repository).findByClienteId(1L);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o cliente não tem agendamentos")
    void meusAgendamentosEmptySuccess() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.findByClienteId(1L)).thenReturn(List.of());

        List<AgendamentoResponse> lista = agendamentoService.meusAgendamentos();

        assertTrue(lista.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando quem consulta os próprios agendamentos não é cliente")
    void meusAgendamentosNotClientError() {
        autenticar(usuarioBarbeiro);
        when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> agendamentoService.meusAgendamentos());

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Deve retornar apenas os agendamentos do barbeiro quando o usuário for barbeiro")
    void findAllBarbeiroSuccess() {
        autenticar(usuarioBarbeiro);
        when(repository.findByBarbeiroId(2L)).thenReturn(List.of(agendamento));

        List<AgendamentoResponse> lista = agendamentoService.findAll();

        assertEquals(1, lista.size());
        verify(repository).findByBarbeiroId(2L);
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o barbeiro não tem agendamentos")
    void findAllBarbeiroEmptySuccess() {
        autenticar(usuarioBarbeiro);
        when(repository.findByBarbeiroId(2L)).thenReturn(List.of());

        List<AgendamentoResponse> lista = agendamentoService.findAll();

        assertTrue(lista.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar todos os agendamentos quando o usuário não for barbeiro")
    void findAllAdminSuccess() {
        autenticar(usuarioComRole(5L, "ROLE_ADMIN"));
        AgendamentoEntity outro = agendamentoNoHorario(11L, LocalTime.of(15, 0), AgendamentoStatus.CONFIRMADO);
        when(repository.findAll()).thenReturn(List.of(agendamento, outro));

        List<AgendamentoResponse> lista = agendamentoService.findAll();

        assertEquals(2, lista.size());
        assertEquals("CONFIRMADO", lista.get(1).status());
        verify(repository).findAll();
        verify(repository, never()).findByBarbeiroId(any());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException no findAll quando o principal não for um usuário")
    void findAllUnauthenticatedError() {
        autenticarPrincipalInvalido();

        assertThrows(ForbiddenException.class, () -> agendamentoService.findAll());

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao atualizar o status de um agendamento inexistente")
    void atualizarStatusNotFoundError() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> agendamentoService.atualizarStatus(99L, AgendamentoStatus.CONFIRMADO));

        assertThat(e.getMessage(), is("Agendamento não encontrado."));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o agendamento já estiver cancelado")
    void atualizarStatusAlreadyCancelledError() {
        agendamento.setStatus(AgendamentoStatus.CANCELADO);
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO));

        assertThat(e.getMessage(), is("Esse agendamento já foi finalizado ou cancelado."));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o principal não for um usuário ao atualizar o status")
    void atualizarStatusUnauthenticatedError() {
        autenticarPrincipalInvalido();
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        assertThrows(ForbiddenException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve permitir que o barbeiro dono confirme o agendamento")
    void atualizarStatusBarbeiroSuccess() {
        autenticar(usuarioBarbeiro);
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.saveAndFlush(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO);

        assertEquals("CONFIRMADO", response.status());
        assertEquals(AgendamentoStatus.CONFIRMADO, agendamento.getStatus());
        verify(pagamentoService, never()).reembolsarPorCancelamento(any());
    }

    @Test
    @DisplayName("Deve reembolsar e avisar a fila quando o barbeiro dono cancela")
    void atualizarStatusBarbeiroCancelSuccess() {
        autenticar(usuarioBarbeiro);
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.saveAndFlush(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO);

        assertEquals("CANCELADO", response.status());
        assertEquals(AgendamentoStatus.CANCELADO, agendamento.getStatus());
        assertFalse(agendamento.getCanceladoPeloCliente());
        verify(filaEsperaService).processarCancelamento(agendamento);
        verify(pagamentoService).reembolsarPorCancelamento(agendamento);
        verify(eventPublisher).publishEvent(any(ComprovanteCancelamentoEvent.class));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando um barbeiro altera o agendamento de outro barbeiro")
    void atualizarStatusOtherBarberError() {
        autenticar(usuarioComRole(7L, "ROLE_BARBEIRO"));
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO));

        assertThat(e.getMessage(), is("Esse agendamento é de outro barbeiro."));
        assertEquals(AgendamentoStatus.PENDENTE, agendamento.getStatus());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve permitir que o administrador confirme qualquer agendamento")
    void atualizarStatusAdminSuccess() {
        autenticar(usuarioComRole(5L, "ROLE_ADMIN"));
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.saveAndFlush(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO);

        assertEquals("CONFIRMADO", response.status());
    }

    @Test
    @DisplayName("Deve permitir que o administrador cancele qualquer agendamento com reembolso")
    void atualizarStatusAdminCancelSuccess() {
        autenticar(usuarioComRole(5L, "ROLE_ADMIN"));
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.saveAndFlush(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO);

        assertEquals("CANCELADO", response.status());
        verify(pagamentoService).reembolsarPorCancelamento(agendamento);
    }

    @Test
    @DisplayName("Deve reembolsar quando o cliente cancela com mais de 2 horas de antecedência")
    void atualizarStatusClientCancelComAntecedencia() {
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.saveAndFlush(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO);

        assertEquals("CANCELADO", response.status());
        assertTrue(agendamento.getCanceladoPeloCliente());
        assertNotNull(agendamento.getCanceladoEm());
        verify(pagamentoService).reembolsarPorCancelamento(agendamento);
    }

    @Test
    @DisplayName("Não deve reembolsar quando o cliente cancela com menos de 2 horas de antecedência")
    void atualizarStatusClientCancelSemAntecedencia() {
        agendamento.setData(LocalDate.now(FUSO));
        agendamento.setHorario(LocalTime.now(FUSO).plusMinutes(30));
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.saveAndFlush(any(AgendamentoEntity.class))).thenReturn(agendamento);

        agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO);

        assertEquals(AgendamentoStatus.CANCELADO, agendamento.getStatus());
        verify(pagamentoService, never()).reembolsarPorCancelamento(any());
    }

    @Test
    @DisplayName("Deve bloquear o cliente por 7 dias ao chegar no terceiro cancelamento do mês")
    void atualizarStatusClientBloqueadoNoTerceiroCancelamento() {
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.saveAndFlush(any(AgendamentoEntity.class))).thenReturn(agendamento);
        when(repository.countByClienteIdAndCanceladoPeloClienteTrueAndCanceladoEmBetween(eq(1L), any(), any()))
                .thenReturn(2L, 3L, 3L);

        agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO);

        assertNotNull(cliente.getBloqueadoAte());
        assertTrue(cliente.getBloqueadoAte().isAfter(LocalDateTime.now(FUSO).plusDays(6)));
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Não deve bloquear o cliente enquanto ele não atingiu o limite de cancelamentos")
    void atualizarStatusClientSemBloqueioAbaixoDoLimite() {
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.saveAndFlush(any(AgendamentoEntity.class))).thenReturn(agendamento);
        when(repository.countByClienteIdAndCanceladoPeloClienteTrueAndCanceladoEmBetween(eq(1L), any(), any()))
                .thenReturn(0L, 1L, 1L);

        agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO);

        assertNull(cliente.getBloqueadoAte());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o cliente já usou o limite de cancelamentos do mês")
    void atualizarStatusClientLimiteAtingido() {
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.countByClienteIdAndCanceladoPeloClienteTrueAndCanceladoEmBetween(eq(1L), any(), any()))
                .thenReturn(3L);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO));

        assertThat(e.getMessage(), is("Você já usou os 3 cancelamentos permitidos neste mês. Entre em contato com a barbearia."));
        assertEquals(AgendamentoStatus.PENDENTE, agendamento.getStatus());
        verify(repository, never()).saveAndFlush(any());
        verify(pagamentoService, never()).reembolsarPorCancelamento(any());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o cliente tentar confirmar")
    void atualizarStatusClientConfirmError() {
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO));

        assertThat(e.getMessage(), is("Clientes só podem cancelar agendamentos."));
        assertEquals(AgendamentoStatus.PENDENTE, agendamento.getStatus());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando um cliente cancela o agendamento de outro cliente")
    void atualizarStatusOtherClientError() {
        ClienteEntity outro = ClienteEntity.builder()
                .id(9L)
                .usuario(usuarioComRole(9L, "ROLE_CLIENTE"))
                .nome("Outro")
                .telefone("79911111111")
                .build();
        agendamento.setCliente(outro);
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CANCELADO));

        assertThat(e.getMessage(), is("Esse agendamento não é seu."));
        assertEquals(AgendamentoStatus.PENDENTE, agendamento.getStatus());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao usar atualizarStatus para concluir o atendimento")
    void atualizarStatusConcluirPeloCaminhoErrado() {
        autenticar(usuarioBarbeiro);
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.atualizarStatus(10L, AgendamentoStatus.CONCLUIDO));

        assertThat(e.getMessage(), is("Use o registro de comparecimento para concluir ou registrar falta."));
    }

    @Test
    @DisplayName("Deve concluir o atendimento e pedir a avaliação ao cliente")
    void registrarComparecimentoConcluido() {
        autenticar(usuarioBarbeiro);
        agendamento.setData(LocalDate.now(FUSO).minusDays(1));
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.save(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.registrarComparecimento(10L, AgendamentoStatus.CONCLUIDO);

        assertEquals("CONCLUIDO", response.status());
        verify(eventPublisher).publishEvent(any(AvaliacaoSolicitadaEvent.class));
        verify(pagamentoService, never()).reembolsarPorFalta(any());
    }

    @Test
    @DisplayName("Deve registrar a falta do cliente e reembolsar o Pix")
    void registrarComparecimentoNaoCompareceu() {
        autenticar(usuarioBarbeiro);
        agendamento.setData(LocalDate.now(FUSO).minusDays(1));
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(repository.save(any(AgendamentoEntity.class))).thenReturn(agendamento);

        AgendamentoResponse response = agendamentoService.registrarComparecimento(10L, AgendamentoStatus.NAO_COMPARECEU);

        assertEquals("NAO_COMPARECEU", response.status());
        verify(pagamentoService).reembolsarPorFalta(agendamento);
        verify(eventPublisher, never()).publishEvent(any(AvaliacaoSolicitadaEvent.class));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao registrar comparecimento antes do horário")
    void registrarComparecimentoAntesDoHorario() {
        autenticar(usuarioBarbeiro);
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.registrarComparecimento(10L, AgendamentoStatus.CONCLUIDO));

        assertThat(e.getMessage(), is("Aguarde o início do horário para registrar o comparecimento."));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o status de comparecimento for inválido")
    void registrarComparecimentoStatusInvalido() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.registrarComparecimento(10L, AgendamentoStatus.CANCELADO));

        assertThat(e.getMessage(), is("Status de comparecimento inválido."));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o cliente tenta registrar comparecimento")
    void registrarComparecimentoClienteNaoPode() {
        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> agendamentoService.registrarComparecimento(10L, AgendamentoStatus.CONCLUIDO));

        assertThat(e.getMessage(), is("Apenas barbeiros e administradores podem registrar comparecimento."));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o barbeiro registra comparecimento de outro barbeiro")
    void registrarComparecimentoOutroBarbeiro() {
        autenticar(usuarioComRole(7L, "ROLE_BARBEIRO"));
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> agendamentoService.registrarComparecimento(10L, AgendamentoStatus.CONCLUIDO));

        assertThat(e.getMessage(), is("Esse agendamento pertence a outro barbeiro."));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao registrar comparecimento de atendimento já finalizado")
    void registrarComparecimentoJaFinalizado() {
        autenticar(usuarioBarbeiro);
        agendamento.setStatus(AgendamentoStatus.CONCLUIDO);
        when(repository.findById(10L)).thenReturn(Optional.of(agendamento));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> agendamentoService.registrarComparecimento(10L, AgendamentoStatus.NAO_COMPARECEU));

        assertThat(e.getMessage(), is("Esse atendimento já foi finalizado ou cancelado."));
    }

    @Test
    @DisplayName("Deve montar o resumo de cancelamentos do cliente com bloqueio ativo")
    void resumoCancelamentosComBloqueio() {
        cliente.setBloqueadoAte(LocalDateTime.now(FUSO).plusDays(3));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.countByClienteIdAndCanceladoPeloClienteTrueAndCanceladoEmBetween(eq(1L), any(), any()))
                .thenReturn(3L);

        ResumoCancelamentosResponse resumo = agendamentoService.resumoCancelamentos();

        assertEquals(3, resumo.usados());
        assertEquals(3, resumo.limite());
        assertNotNull(resumo.bloqueadoAte());
    }

    @Test
    @DisplayName("Deve ignorar bloqueio já expirado no resumo de cancelamentos")
    void resumoCancelamentosBloqueioExpirado() {
        cliente.setBloqueadoAte(LocalDateTime.now(FUSO).minusDays(1));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        ResumoCancelamentosResponse resumo = agendamentoService.resumoCancelamentos();

        assertEquals(0, resumo.usados());
        assertNull(resumo.bloqueadoAte());
    }
}
