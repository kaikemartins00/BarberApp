package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoConfirmadoDto;
import com.kaikeMartins.barberapp_backend.dto.PagamentoResponse;
import com.kaikeMartins.barberapp_backend.event.PagamentoAprovadoEvent;
import com.kaikeMartins.barberapp_backend.event.ReembolsoEvent;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @InjectMocks
    private PagamentoService service;

    @Mock
    private PagamentoRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MercadoPagoClient mercadoPago;

    private ClienteEntity cliente;
    private BarbeiroEntity barbeiro;
    private ServicoEntity servico;
    private AgendamentoEntity agendamento;
    private PagamentoEntity pagamentoPix;

    @BeforeEach
    void setUp() {
        cliente = cliente(1L);
        barbeiro = barbeiro(2L);
        servico = servico(3L, 40.0);
        agendamento = agendamento(10L, cliente, barbeiro, servico,
                LocalDate.now().plusDays(1), LocalTime.of(14, 0), AgendamentoStatus.CONFIRMADO);
        pagamentoPix = PagamentoEntity.builder()
                .id(100L)
                .agendamento(agendamento)
                .valor(new BigDecimal("40.00"))
                .metodo(PagamentoMetodo.PIX)
                .status(PagamentoStatus.PAGO)
                .mercadoPagoId("mp-1")
                .build();
        autenticar(cliente.getUsuario());
    }

    @AfterEach
    void tearDown() {
        limparAutenticacao();
    }

    private PagamentoEntity pagamentoPresencial(PagamentoStatus status) {
        return PagamentoEntity.builder()
                .id(101L)
                .agendamento(agendamento)
                .valor(new BigDecimal("40.00"))
                .metodo(PagamentoMetodo.PRESENCIAL)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("Deve criar o pagamento presencial pendente com o preço do serviço")
    void criarPagamentoSuccess() {
        when(repository.existsByAgendamentoId(10L)).thenReturn(false);

        service.criarPagamento(agendamento);

        ArgumentCaptor<PagamentoEntity> captor = ArgumentCaptor.forClass(PagamentoEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(PagamentoMetodo.PRESENCIAL, captor.getValue().getMetodo());
        assertEquals(PagamentoStatus.PENDENTE, captor.getValue().getStatus());
        assertEquals(new BigDecimal("40.00"), captor.getValue().getValor());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o agendamento já possui pagamento")
    void criarPagamentoDuplicado() {
        when(repository.existsByAgendamentoId(10L)).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.criarPagamento(agendamento));

        assertEquals("Esse agendamento já possui pagamento.", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o serviço custa zero")
    void criarPagamentoValorZero() {
        servico.setPreco(0.0);
        when(repository.existsByAgendamentoId(10L)).thenReturn(false);

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.criarPagamento(agendamento));

        assertEquals("O valor do serviço deve ser maior que zero.", e.getMessage());
    }

    @Test
    @DisplayName("Deve reembolsar o Pix pago no cancelamento e registrar o reembolso")
    void reembolsarPorCancelamentoSuccess() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPix));

        service.reembolsarPorCancelamento(agendamento);

        verify(mercadoPago).reembolsar("mp-1");
        assertEquals(PagamentoStatus.CANCELADO, pagamentoPix.getStatus());
        assertNotNull(pagamentoPix.getReembolsadoEm());
        assertFalse(pagamentoPix.getReembolsoPendente());
        verify(repository).save(pagamentoPix);
        verify(eventPublisher).publishEvent(any(ReembolsoEvent.class));
    }

    @Test
    @DisplayName("Não deve reembolsar pagamento presencial")
    void reembolsarPorCancelamentoPresencial() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PAGO)));

        service.reembolsarPorCancelamento(agendamento);

        verify(mercadoPago, never()).reembolsar(any());
        verify(eventPublisher, never()).publishEvent(any(ReembolsoEvent.class));
    }

    @Test
    @DisplayName("Não deve reembolsar Pix que ainda não foi pago")
    void reembolsarPorCancelamentoPixPendente() {
        pagamentoPix.setStatus(PagamentoStatus.PENDENTE);
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPix));

        service.reembolsarPorCancelamento(agendamento);

        verify(mercadoPago, never()).reembolsar(any());
    }

    @Test
    @DisplayName("Não deve reembolsar quando o agendamento não tem pagamento")
    void reembolsarPorCancelamentoSemPagamento() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.empty());

        service.reembolsarPorCancelamento(agendamento);

        verify(mercadoPago, never()).reembolsar(any());
    }

    @Test
    @DisplayName("Deve marcar o reembolso como pendente quando o Mercado Pago falha no cancelamento")
    void reembolsarPorCancelamentoFalhaViraPendente() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPix));
        when(mercadoPago.reembolsar("mp-1")).thenThrow(new IllegalStateException("indisponível"));

        service.reembolsarPorCancelamento(agendamento);

        assertTrue(pagamentoPix.getReembolsoPendente());
        assertEquals(PagamentoStatus.PAGO, pagamentoPix.getStatus());
        verify(repository).save(pagamentoPix);
        verify(eventPublisher, never()).publishEvent(any(ReembolsoEvent.class));
    }

    @Test
    @DisplayName("Deve reembolsar o Pix quando o cliente não comparece")
    void reembolsarPorFaltaSuccess() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPix));

        assertTrue(service.reembolsarPorFalta(agendamento));

        verify(mercadoPago).reembolsar("mp-1");
        assertEquals(PagamentoStatus.CANCELADO, pagamentoPix.getStatus());
    }

    @Test
    @DisplayName("Deve devolver false quando não há Pix para reembolsar na falta")
    void reembolsarPorFaltaSemPix() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PENDENTE)));

        assertFalse(service.reembolsarPorFalta(agendamento));

        verify(mercadoPago, never()).reembolsar(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o reembolso da falta falha")
    void reembolsarPorFaltaFalha() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPix));
        when(mercadoPago.reembolsar("mp-1")).thenThrow(new IllegalStateException("indisponível"));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.reembolsarPorFalta(agendamento));

        assertEquals("Não foi possível reembolsar o Pix agora. Tente novamente em instantes.", e.getMessage());
    }

    @Test
    @DisplayName("Deve informar quando o agendamento pode ser reembolsado")
    void podeReembolsar() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPix));

        assertTrue(service.podeReembolsar(agendamento));
    }

    @Test
    @DisplayName("Deve reprocessar reembolsos pendentes e limpar os que não são mais elegíveis")
    void reprocessarReembolsosPendentes() {
        PagamentoEntity elegivel = pagamentoPix;
        elegivel.setReembolsoPendente(true);
        PagamentoEntity inelegivel = pagamentoPresencial(PagamentoStatus.PENDENTE);
        inelegivel.setReembolsoPendente(true);
        when(repository.findByReembolsoPendenteTrue()).thenReturn(List.of(elegivel, inelegivel));

        service.reprocessarReembolsosPendentes();

        verify(mercadoPago).reembolsar("mp-1");
        assertEquals(PagamentoStatus.CANCELADO, elegivel.getStatus());
        assertFalse(inelegivel.getReembolsoPendente());
    }

    @Test
    @DisplayName("Deve manter o reembolso pendente quando o reprocessamento ainda falha")
    void reprocessarReembolsosPendentesAindaFalha() {
        pagamentoPix.setReembolsoPendente(true);
        when(repository.findByReembolsoPendenteTrue()).thenReturn(List.of(pagamentoPix));
        when(mercadoPago.reembolsar("mp-1")).thenThrow(new IllegalStateException("indisponível"));

        service.reprocessarReembolsosPendentes();

        assertTrue(pagamentoPix.getReembolsoPendente());
        assertEquals(PagamentoStatus.PAGO, pagamentoPix.getStatus());
    }

    @Test
    @DisplayName("Deve confirmar o Pix validado, confirmar o agendamento e publicar os eventos")
    void confirmarPixValidadoSuccess() {
        pagamentoPix.setStatus(PagamentoStatus.AGUARDANDO_CONFIRMACAO);
        agendamento.setStatus(AgendamentoStatus.PENDENTE);
        when(repository.findByMercadoPagoId("mp-1")).thenReturn(Optional.of(pagamentoPix));

        service.confirmarPixValidado("mp-1", new BigDecimal("40.00"), "100");

        assertEquals(PagamentoStatus.PAGO, pagamentoPix.getStatus());
        assertNotNull(pagamentoPix.getConfirmadoEm());
        assertEquals(AgendamentoStatus.CONFIRMADO, agendamento.getStatus());
        verify(eventPublisher).publishEvent(any(AgendamentoConfirmadoDto.class));
        verify(eventPublisher).publishEvent(any(PagamentoAprovadoEvent.class));
    }

    @Test
    @DisplayName("Deve ignorar a confirmação de um Pix que já estava pago")
    void confirmarPixValidadoJaPago() {
        when(repository.findByMercadoPagoId("mp-1")).thenReturn(Optional.of(pagamentoPix));

        service.confirmarPixValidado("mp-1", new BigDecimal("40.00"), "100");

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(PagamentoAprovadoEvent.class));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o valor do Pix não confere")
    void confirmarPixValidadoValorDiferente() {
        pagamentoPix.setStatus(PagamentoStatus.AGUARDANDO_CONFIRMACAO);
        when(repository.findByMercadoPagoId("mp-1")).thenReturn(Optional.of(pagamentoPix));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.confirmarPixValidado("mp-1", new BigDecimal("10.00"), "100"));

        assertEquals("Cobrança Pix não corresponde ao pagamento registrado.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando a referência do Pix não confere")
    void confirmarPixValidadoReferenciaDiferente() {
        pagamentoPix.setStatus(PagamentoStatus.AGUARDANDO_CONFIRMACAO);
        when(repository.findByMercadoPagoId("mp-1")).thenReturn(Optional.of(pagamentoPix));

        assertThrows(BadRequestException.class,
                () -> service.confirmarPixValidado("mp-1", new BigDecimal("40.00"), "999"));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o agendamento do Pix foi cancelado")
    void confirmarPixValidadoAgendamentoCancelado() {
        pagamentoPix.setStatus(PagamentoStatus.AGUARDANDO_CONFIRMACAO);
        agendamento.setStatus(AgendamentoStatus.CANCELADO);
        when(repository.findByMercadoPagoId("mp-1")).thenReturn(Optional.of(pagamentoPix));

        assertThrows(BadRequestException.class,
                () -> service.confirmarPixValidado("mp-1", new BigDecimal("40.00"), "100"));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a cobrança Pix não existe")
    void confirmarPixValidadoNaoEncontrado() {
        when(repository.findByMercadoPagoId("mp-x")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.confirmarPixValidado("mp-x", new BigDecimal("40.00"), "100"));
    }

    @Test
    @DisplayName("Deve permitir que o cliente escolha Pix como forma de pagamento")
    void escolherMetodoSuccess() {
        PagamentoEntity pendente = pagamentoPresencial(PagamentoStatus.PENDENTE);
        agendamento.setStatus(AgendamentoStatus.PENDENTE);
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pendente));
        when(repository.save(pendente)).thenReturn(pendente);

        PagamentoResponse response = service.escolherMetodo(10L, PagamentoMetodo.PIX);

        assertEquals("PIX", response.metodo());
        assertEquals("PENDENTE", response.status());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando outro cliente tenta escolher o método")
    void escolherMetodoOutroCliente() {
        autenticar(usuario(9L, CLIENTE));
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PENDENTE)));

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> service.escolherMetodo(10L, PagamentoMetodo.PIX));

        assertEquals("Somente o cliente pode escolher a forma de pagamento.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o método de pagamento não é informado")
    void escolherMetodoNulo() {
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PENDENTE)));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.escolherMetodo(10L, null));

        assertEquals("Informe a forma de pagamento.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o prazo para alterar o pagamento terminou")
    void escolherMetodoPrazoTerminou() {
        agendamento.setData(LocalDate.now().minusDays(1));
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PENDENTE)));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.escolherMetodo(10L, PagamentoMetodo.PIX));

        assertEquals("O prazo para alterar o pagamento terminou.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o pagamento já não está pendente")
    void escolherMetodoPagamentoJaPago() {
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PAGO)));

        assertThrows(BadRequestException.class, () -> service.escolherMetodo(10L, PagamentoMetodo.PIX));
    }

    @Test
    @DisplayName("Deve permitir que o barbeiro responsável confirme o pagamento presencial")
    void confirmarBarbeiroSuccess() {
        PagamentoEntity pendente = pagamentoPresencial(PagamentoStatus.PENDENTE);
        autenticar(barbeiro.getUsuario());
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pendente));
        when(repository.save(pendente)).thenReturn(pendente);

        PagamentoResponse response = service.confirmar(10L);

        assertEquals("PAGO", response.status());
        assertSame(barbeiro.getUsuario(), pendente.getConfirmadoPor());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o cliente tenta confirmar o pagamento")
    void confirmarClienteNaoPode() {
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PENDENTE)));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.confirmar(10L));

        assertEquals("Você não pode confirmar esse pagamento.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao confirmar um pagamento já pago")
    void confirmarJaPago() {
        autenticar(usuario(5L, ADMIN));
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PAGO)));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.confirmar(10L));

        assertEquals("Esse pagamento já foi confirmado.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao confirmar manualmente um pagamento Pix")
    void confirmarPixManual() {
        autenticar(usuario(5L, ADMIN));
        pagamentoPix.setStatus(PagamentoStatus.PENDENTE);
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPix));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.confirmar(10L));

        assertEquals("O Pix é confirmado automaticamente pelo provedor.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao confirmar pagamento de agendamento cancelado")
    void confirmarAgendamentoCancelado() {
        autenticar(usuario(5L, ADMIN));
        agendamento.setStatus(AgendamentoStatus.CANCELADO);
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PENDENTE)));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.confirmar(10L));

        assertEquals("Não é possível confirmar um pagamento cancelado.", e.getMessage());
    }

    @Test
    @DisplayName("Deve consultar o pagamento quando o usuário é o cliente do agendamento")
    void consultarCliente() {
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPix));

        PagamentoResponse response = service.consultar(10L);

        assertEquals(10L, response.agendamentoId());
        assertEquals("PIX", response.metodo());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando um estranho consulta o pagamento")
    void consultarEstranho() {
        autenticar(usuario(9L, CLIENTE));
        when(repository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamentoPix));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.consultar(10L));

        assertEquals("Você não tem acesso a esse pagamento.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o pagamento não existe")
    void consultarNaoEncontrado() {
        when(repository.findByAgendamentoId(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.consultar(99L));
    }

    @Test
    @DisplayName("Deve confirmar a reserva presencial pendente")
    void confirmarReservaPresencialSuccess() {
        agendamento.setStatus(AgendamentoStatus.PENDENTE);
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPresencial(PagamentoStatus.PENDENTE)));

        service.confirmarReservaPresencial(10L);

        assertEquals(AgendamentoStatus.CONFIRMADO, agendamento.getStatus());
        verify(eventPublisher).publishEvent(any(AgendamentoConfirmadoDto.class));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando a reserva presencial é inválida")
    void confirmarReservaPresencialInvalida() {
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamentoPix));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.confirmarReservaPresencial(10L));

        assertEquals("Reserva presencial inválida.", e.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar a confirmação manual do Pix informado pelo cliente")
    void informarPixRejeitado() {
        BadRequestException e = assertThrows(BadRequestException.class, () -> service.informarPix(10L));

        assertEquals("O Pix é confirmado automaticamente pelo Mercado Pago.", e.getMessage());
    }
}