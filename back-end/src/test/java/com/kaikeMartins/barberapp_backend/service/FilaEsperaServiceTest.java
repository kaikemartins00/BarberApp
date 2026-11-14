package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.FilaEsperaEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.FilaEsperaStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.FilaEsperaRequest;
import com.kaikeMartins.barberapp_backend.dto.FilaEsperaResponse;
import com.kaikeMartins.barberapp_backend.event.VagaFilaEvent;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.FilaEsperaRepository;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilaEsperaServiceTest {

    private static final LocalTime DEZ_HORAS = LocalTime.of(10, 0);

    @InjectMocks
    private FilaEsperaService service;

    @Mock
    private FilaEsperaRepository filaRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ClienteEntity cliente;
    private BarbeiroEntity barbeiro;
    private ServicoEntity servico;
    private AgendamentoEntity alternativo;
    private LocalDate diaDesejado;
    private LocalDate diaAlternativo;

    @BeforeEach
    void setUp() {
        diaDesejado = LocalDate.now().plusDays(2);
        diaAlternativo = LocalDate.now().plusDays(5);
        cliente = cliente(1L);
        barbeiro = barbeiro(2L);
        servico = servico(3L, 40.0);
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(diaDesejado, DEZ_HORAS));
        alternativo = agendamento(20L, cliente, barbeiro, servico, diaAlternativo,
                LocalTime.of(14, 0), AgendamentoStatus.CONFIRMADO);
        autenticar(cliente.getUsuario());
    }

    @AfterEach
    void tearDown() {
        limparAutenticacao();
    }

    private FilaEsperaRequest pedido(LocalDate data, LocalTime... horarios) {
        return new FilaEsperaRequest(20L, data, Set.of(horarios));
    }

    private void prepararEntradaValida() {
        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(alternativo));
        when(agendamentoRepository.findByClienteId(1L)).thenReturn(List.of(alternativo));
        when(filaRepository.existsByAgendamentoIdAndStatus(20L, FilaEsperaStatus.AGUARDANDO)).thenReturn(false);
    }

    private FilaEsperaEntity inscricao(Long id, FilaEsperaStatus status) {
        return FilaEsperaEntity.builder()
                .id(id)
                .agendamento(alternativo)
                .dataPreferida(diaDesejado)
                .horariosAceitos(new HashSet<>(Set.of(DEZ_HORAS)))
                .status(status)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve entrar na fila quando o horário desejado está ocupado")
    void entrarSuccess() {
        prepararEntradaValida();
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                2L, diaDesejado, DEZ_HORAS, AgendamentoStatus.CANCELADO)).thenReturn(true);
        when(filaRepository.save(any(FilaEsperaEntity.class))).thenAnswer(i -> i.getArgument(0));

        FilaEsperaResponse resposta = service.entrar(pedido(diaDesejado, DEZ_HORAS));

        assertEquals("AGUARDANDO", resposta.status());
        assertEquals(diaDesejado, resposta.dataPreferida());
        assertEquals(Set.of(DEZ_HORAS), resposta.horariosAceitos());
        assertEquals(diaAlternativo, resposta.dataAgendada());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o agendamento alternativo não existe")
    void entrarAgendamentoInexistente() {
        when(agendamentoRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.entrar(pedido(diaDesejado, DEZ_HORAS)));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o agendamento alternativo é de outro cliente")
    void entrarAgendamentoDeOutro() {
        autenticar(usuario(9L, CLIENTE));
        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(alternativo));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> service.entrar(pedido(diaDesejado, DEZ_HORAS)));

        assertEquals("Esse agendamento não pertence a você.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o agendamento alternativo não está ativo")
    void entrarAlternativoInativo() {
        alternativo.setStatus(AgendamentoStatus.CANCELADO);
        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(alternativo));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.entrar(pedido(diaDesejado, DEZ_HORAS)));

        assertEquals("O agendamento alternativo não está ativo.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o agendamento alternativo já passou")
    void entrarAlternativoPassado() {
        alternativo.setData(LocalDate.now().minusDays(1));
        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(alternativo));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.entrar(pedido(diaDesejado, DEZ_HORAS)));

        assertEquals("O agendamento alternativo já passou.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o cliente já tem horário no dia desejado")
    void entrarJaTemHorarioNoDia() {
        AgendamentoEntity nodia = agendamento(21L, cliente, barbeiro, servico, diaDesejado,
                LocalTime.of(16, 0), AgendamentoStatus.CONFIRMADO);
        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(alternativo));
        when(agendamentoRepository.findByClienteId(1L)).thenReturn(List.of(alternativo, nodia));

        assertThrows(BadRequestException.class, () -> service.entrar(pedido(diaDesejado, DEZ_HORAS)));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando a data preferida não é anterior à alternativa")
    void entrarDataPreferidaNaoAnterior() {
        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(alternativo));
        when(agendamentoRepository.findByClienteId(1L)).thenReturn(List.of(alternativo));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.entrar(pedido(diaAlternativo.plusDays(1), DEZ_HORAS)));

        assertEquals("A data preferida deve ser anterior à alternativa.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o cliente já está na fila desse agendamento")
    void entrarJaEstaNaFila() {
        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(alternativo));
        when(agendamentoRepository.findByClienteId(1L)).thenReturn(List.of(alternativo));
        when(filaRepository.existsByAgendamentoIdAndStatus(20L, FilaEsperaStatus.AGUARDANDO)).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.entrar(pedido(diaDesejado, DEZ_HORAS)));

        assertEquals("Você já está na fila desse agendamento.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o horário escolhido não está na agenda do barbeiro")
    void entrarHorarioForaDaAgenda() {
        prepararEntradaValida();
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.entrar(pedido(diaDesejado, LocalTime.of(11, 0))));

        assertEquals("Um dos horários escolhidos não pertence à agenda do barbeiro.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando existe horário livre na data desejada")
    void entrarComHorarioLivre() {
        prepararEntradaValida();
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                2L, diaDesejado, DEZ_HORAS, AgendamentoStatus.CANCELADO)).thenReturn(false);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.entrar(pedido(diaDesejado, DEZ_HORAS)));

        assertEquals("Existe um horário livre na data desejada. Faça o agendamento diretamente.", e.getMessage());
        verify(filaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve sair da fila quando a inscrição é do cliente e está aguardando")
    void sairSuccess() {
        FilaEsperaEntity fila = inscricao(50L, FilaEsperaStatus.AGUARDANDO);
        when(filaRepository.findById(50L)).thenReturn(Optional.of(fila));
        when(filaRepository.save(fila)).thenReturn(fila);

        FilaEsperaResponse resposta = service.sair(50L);

        assertEquals("CANCELADO", resposta.status());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando outro cliente tenta sair da fila")
    void sairDeOutroCliente() {
        autenticar(usuario(9L, CLIENTE));
        when(filaRepository.findById(50L)).thenReturn(Optional.of(inscricao(50L, FilaEsperaStatus.AGUARDANDO)));

        assertThrows(ForbiddenException.class, () -> service.sair(50L));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao sair de uma inscrição que não está aguardando")
    void sairJaAtendida() {
        when(filaRepository.findById(50L)).thenReturn(Optional.of(inscricao(50L, FilaEsperaStatus.ATENDIDO)));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.sair(50L));

        assertEquals("Essa inscrição não está mais aguardando.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao sair de inscrição inexistente")
    void sairInexistente() {
        when(filaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.sair(99L));
    }

    @Test
    @DisplayName("Deve consultar a inscrição quando o usuário é o cliente")
    void consultarCliente() {
        when(filaRepository.findById(50L)).thenReturn(Optional.of(inscricao(50L, FilaEsperaStatus.AGUARDANDO)));

        FilaEsperaResponse resposta = service.consultar(50L);

        assertEquals(50L, resposta.id());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando um estranho consulta a inscrição")
    void consultarEstranho() {
        autenticar(usuario(9L, CLIENTE));
        when(filaRepository.findById(50L)).thenReturn(Optional.of(inscricao(50L, FilaEsperaStatus.AGUARDANDO)));

        assertThrows(ForbiddenException.class, () -> service.consultar(50L));
    }

    private AgendamentoEntity vagaCancelada(LocalDate dia) {
        return agendamento(30L, cliente(8L), barbeiro, servico, dia, DEZ_HORAS, AgendamentoStatus.CANCELADO);
    }

    private PagamentoEntity pagamentoAtivo() {
        return PagamentoEntity.builder()
                .id(200L)
                .agendamento(alternativo)
                .valor(new BigDecimal("40.00"))
                .metodo(PagamentoMetodo.PRESENCIAL)
                .status(PagamentoStatus.PENDENTE)
                .build();
    }

    @Test
    @DisplayName("Deve mover o primeiro da fila para a vaga cancelada e avisá-lo")
    void processarCancelamentoMoveOPrimeiroDaFila() {
        FilaEsperaEntity fila = inscricao(50L, FilaEsperaStatus.AGUARDANDO);
        when(filaRepository.buscarFila(2L, diaDesejado, FilaEsperaStatus.AGUARDANDO)).thenReturn(List.of(fila));
        when(pagamentoRepository.findByAgendamentoId(20L)).thenReturn(Optional.of(pagamentoAtivo()));
        when(agendamentoRepository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                2L, diaDesejado, DEZ_HORAS, AgendamentoStatus.CANCELADO)).thenReturn(false);

        service.processarCancelamento(vagaCancelada(diaDesejado));

        assertEquals(diaDesejado, alternativo.getData());
        assertEquals(DEZ_HORAS, alternativo.getHorario());
        assertEquals(FilaEsperaStatus.ATENDIDO, fila.getStatus());
        assertNotNull(fila.getAtendidoEm());
        verify(agendamentoRepository).saveAndFlush(alternativo);
        verify(eventPublisher).publishEvent(any(VagaFilaEvent.class));
    }

    @Test
    @DisplayName("Não deve mover ninguém quando o horário da vaga não é aceito pela inscrição")
    void processarCancelamentoHorarioNaoAceito() {
        FilaEsperaEntity fila = inscricao(50L, FilaEsperaStatus.AGUARDANDO);
        fila.setHorariosAceitos(new HashSet<>(Set.of(LocalTime.of(15, 0))));
        when(filaRepository.buscarFila(2L, diaDesejado, FilaEsperaStatus.AGUARDANDO)).thenReturn(List.of(fila));

        service.processarCancelamento(vagaCancelada(diaDesejado));

        verify(agendamentoRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any(VagaFilaEvent.class));
    }

    @Test
    @DisplayName("Não deve mover ninguém quando o pagamento do agendamento alternativo foi cancelado")
    void processarCancelamentoPagamentoCancelado() {
        FilaEsperaEntity fila = inscricao(50L, FilaEsperaStatus.AGUARDANDO);
        PagamentoEntity cancelado = pagamentoAtivo();
        cancelado.setStatus(PagamentoStatus.CANCELADO);
        when(filaRepository.buscarFila(2L, diaDesejado, FilaEsperaStatus.AGUARDANDO)).thenReturn(List.of(fila));
        when(pagamentoRepository.findByAgendamentoId(20L)).thenReturn(Optional.of(cancelado));

        service.processarCancelamento(vagaCancelada(diaDesejado));

        verify(agendamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Não deve mover ninguém quando a vaga já foi ocupada por outro cliente")
    void processarCancelamentoVagaJaOcupada() {
        FilaEsperaEntity fila = inscricao(50L, FilaEsperaStatus.AGUARDANDO);
        when(filaRepository.buscarFila(2L, diaDesejado, FilaEsperaStatus.AGUARDANDO)).thenReturn(List.of(fila));
        when(pagamentoRepository.findByAgendamentoId(20L)).thenReturn(Optional.of(pagamentoAtivo()));
        when(agendamentoRepository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                2L, diaDesejado, DEZ_HORAS, AgendamentoStatus.CANCELADO)).thenReturn(true);

        service.processarCancelamento(vagaCancelada(diaDesejado));

        verify(agendamentoRepository, never()).saveAndFlush(any());
        assertEquals(FilaEsperaStatus.AGUARDANDO, fila.getStatus());
    }

    @Test
    @DisplayName("Não deve procurar a fila quando a vaga cancelada já passou")
    void processarCancelamentoVagaNoPassado() {
        service.processarCancelamento(vagaCancelada(LocalDate.now().minusDays(1)));

        verify(filaRepository, never()).buscarFila(any(), any(), any());
    }

    @Test
    @DisplayName("Deve cancelar as inscrições que dependiam do agendamento cancelado")
    void processarCancelamentoCancelaInscricoesDoCancelado() {
        AgendamentoEntity cancelado = vagaCancelada(LocalDate.now().minusDays(1));
        FilaEsperaEntity dependente = inscricao(60L, FilaEsperaStatus.AGUARDANDO);
        when(filaRepository.findByAgendamentoIdAndStatus(30L, FilaEsperaStatus.AGUARDANDO))
                .thenReturn(List.of(dependente));

        service.processarCancelamento(cancelado);

        assertEquals(FilaEsperaStatus.CANCELADO, dependente.getStatus());
        verify(filaRepository).save(dependente);
    }
}
