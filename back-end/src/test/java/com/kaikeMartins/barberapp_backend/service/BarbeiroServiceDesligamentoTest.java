package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroDesligadoResponse;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroResponse;
import com.kaikeMartins.barberapp_backend.dto.CancelamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.DesligarBarbeiroRequest;
import com.kaikeMartins.barberapp_backend.event.AgendamentosCanceladosEvent;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import com.kaikeMartins.barberapp_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarbeiroServiceDesligamentoTest {

    @InjectMocks
    private BarbeiroService service;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PagamentoService pagamentoService;

    private BarbeiroEntity barbeiro;
    private ClienteEntity cliente;
    private ServicoEntity servico;
    private AgendamentoEntity futuro;
    private AgendamentoEntity outroFuturo;
    private AgendamentoEntity passado;

    @BeforeEach
    void setUp() {
        barbeiro = barbeiro(2L);
        cliente = cliente(1L);
        servico = servico(3L, 40.0);
        barbeiro.getHorariosTrabalho().add(LocalDateTime.now().plusDays(1));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.now().plusDays(2));
        futuro = agendamento(10L, cliente, barbeiro, servico, LocalDate.now().plusDays(1),
                LocalTime.of(14, 0), AgendamentoStatus.CONFIRMADO);
        outroFuturo = agendamento(11L, cliente, barbeiro, servico, LocalDate.now().plusDays(2),
                LocalTime.of(9, 0), AgendamentoStatus.PENDENTE);
        passado = agendamento(12L, cliente, barbeiro, servico, LocalDate.now().minusDays(1),
                LocalTime.of(9, 0), AgendamentoStatus.PENDENTE);
    }

    private void listarAgendamentosAtivos(AgendamentoEntity... agendamentos) {
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusIn(eq(2L), any(), any(), any()))
                .thenReturn(List.of(agendamentos));
    }

    @Test
    @DisplayName("Deve desligar o barbeiro guardando o motivo e bloqueando o acesso")
    void desligarSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        listarAgendamentosAtivos();

        CancelamentoResponse resposta = service.desligar(2L, new DesligarBarbeiroRequest("  Pedido de demissão  "));

        assertNotNull(barbeiro.getDesligadoEm());
        assertEquals("Pedido de demissão", barbeiro.getMotivoDesligamento());
        assertTrue(barbeiro.getHorariosTrabalho().isEmpty());
        assertFalse(barbeiro.getUsuario().isVerificado());
        assertEquals(0, resposta.agendamentosCancelados());
        assertEquals(2, resposta.horariosRemovidos());
        verify(barbeiroRepository).save(barbeiro);
        verify(userRepository).save(barbeiro.getUsuario());
    }

    @Test
    @DisplayName("Deve cancelar e reembolsar apenas os agendamentos futuros e avisar os clientes")
    void desligarCancelaAgendaFutura() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        listarAgendamentosAtivos(futuro, outroFuturo, passado);

        CancelamentoResponse resposta = service.desligar(2L, new DesligarBarbeiroRequest("Mudou de cidade"));

        assertEquals(2, resposta.agendamentosCancelados());
        assertEquals(AgendamentoStatus.CANCELADO, futuro.getStatus());
        assertEquals(AgendamentoStatus.CANCELADO, outroFuturo.getStatus());
        assertEquals(AgendamentoStatus.PENDENTE, passado.getStatus());
        verify(pagamentoService).reembolsarPorCancelamento(futuro);
        verify(pagamentoService).reembolsarPorCancelamento(outroFuturo);
        verify(pagamentoService, never()).reembolsarPorCancelamento(passado);

        ArgumentCaptor<AgendamentosCanceladosEvent> captor = ArgumentCaptor.forClass(AgendamentosCanceladosEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(2, captor.getValue().avisos().size());
        assertEquals("usuario1@email.com", captor.getValue().avisos().get(0).email());
        assertFalse(captor.getValue().avisos().get(0).motivo().contains("Mudou de cidade"));
    }

    @Test
    @DisplayName("Não deve publicar aviso quando não há agendamentos futuros")
    void desligarSemAgendaNaoAvisa() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        listarAgendamentosAtivos();

        service.desligar(2L, new DesligarBarbeiroRequest("Desempenho"));

        verify(eventPublisher, never()).publishEvent(any(AgendamentosCanceladosEvent.class));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao desligar um barbeiro já desligado")
    void desligarJaDesligado() {
        barbeiro.setDesligadoEm(LocalDateTime.now().minusDays(3));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.desligar(2L, new DesligarBarbeiroRequest("Outro motivo")));

        assertEquals("Este barbeiro já foi desligado.", e.getMessage());
        verify(barbeiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao desligar barbeiro inexistente")
    void desligarInexistente() {
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.desligar(99L, new DesligarBarbeiroRequest("Motivo")));
    }

    @Test
    @DisplayName("Deve esconder barbeiros desligados da listagem geral")
    void findAllIgnoraDesligados() {
        BarbeiroEntity desligado = barbeiro(3L);
        desligado.setDesligadoEm(LocalDateTime.now().minusDays(1));
        when(barbeiroRepository.findAll()).thenReturn(List.of(barbeiro, desligado));

        List<BarbeiroResponse> lista = service.findAll();

        assertEquals(1, lista.size());
        assertEquals(2L, lista.get(0).id());
    }

    @Test
    @DisplayName("Deve listar os desligados do mais recente para o mais antigo com o motivo")
    void listarDesligados() {
        BarbeiroEntity antigo = barbeiro(3L);
        antigo.setDesligadoEm(LocalDateTime.now().minusDays(10));
        antigo.setMotivoDesligamento("Faltas");
        BarbeiroEntity recente = barbeiro(4L);
        recente.setDesligadoEm(LocalDateTime.now().minusDays(1));
        recente.setMotivoDesligamento("Pediu demissão");
        when(barbeiroRepository.findAll()).thenReturn(List.of(barbeiro, antigo, recente));

        List<BarbeiroDesligadoResponse> lista = service.listarDesligados();

        assertEquals(2, lista.size());
        assertEquals(4L, lista.get(0).id());
        assertEquals("Pediu demissão", lista.get(0).motivo());
        assertEquals(3L, lista.get(1).id());
        assertEquals("usuario3@email.com", lista.get(1).email());
    }
}
