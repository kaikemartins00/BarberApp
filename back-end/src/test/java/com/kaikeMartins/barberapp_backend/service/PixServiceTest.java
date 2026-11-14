package com.kaikeMartins.barberapp_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.PixResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixServiceTest {

    private static final String WEBHOOK = "https://exemplo.com/webhook";

    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private PixService service;

    @Mock
    private PagamentoService pagamentoService;

    @Mock
    private PagamentoRepository repository;

    @Mock
    private MercadoPagoClient mercadoPago;

    private PagamentoEntity pagamento;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "webhookUrl", WEBHOOK);
        AgendamentoEntity agendamento = agendamento(10L, cliente(1L), barbeiro(2L), servico(3L, 40.0),
                LocalDate.now().plusDays(1), LocalTime.of(14, 0), AgendamentoStatus.PENDENTE);
        pagamento = PagamentoEntity.builder()
                .id(100L)
                .agendamento(agendamento)
                .valor(new BigDecimal("40.00"))
                .metodo(PagamentoMetodo.PIX)
                .status(PagamentoStatus.PENDENTE)
                .build();
    }

    private JsonNode json(String texto) throws Exception {
        return mapper.readTree(texto);
    }

    private JsonNode cobrancaCompleta() throws Exception {
        return json("{\"id\":\"ORD1\",\"transactions\":{\"payments\":[{\"payment_method\":"
                + "{\"qr_code\":\"000201pix\",\"qr_code_base64\":\"QUJD\"}}]}}");
    }

    private String ordemPaga(String id, String status, String statusDetail, String metodo, String referencia, String valor) {
        return "{\"id\":\"" + id + "\",\"status\":\"" + status + "\",\"external_reference\":\"" + referencia + "\","
                + "\"transactions\":{\"payments\":[{\"status\":\"processed\",\"status_detail\":\"" + statusDetail + "\","
                + "\"amount\":\"" + valor + "\",\"payment_method\":{\"id\":\"" + metodo + "\"}}]}}";
    }

    @Test
    @DisplayName("Deve criar a cobrança Pix e guardar o QR Code no pagamento")
    void gerarCriaCobranca() throws Exception {
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamento));
        when(mercadoPago.criar(new BigDecimal("40.00"), 100L, "usuario1@email.com", WEBHOOK)).thenReturn(cobrancaCompleta());

        PixResponse resposta = service.gerarPorAgendamento(10L);

        assertEquals("ORD1", pagamento.getMercadoPagoId());
        assertEquals(PagamentoStatus.AGUARDANDO_CONFIRMACAO, pagamento.getStatus());
        assertEquals("QUJD", resposta.qrCodeBase64());
        assertEquals("000201pix", resposta.pixCopiaECola());
        assertEquals(10L, resposta.agendamentoId());
        verify(pagamentoService).consultar(10L);
        verify(repository).save(pagamento);
    }

    @Test
    @DisplayName("Deve reaproveitar a cobrança já criada sem chamar o Mercado Pago de novo")
    void gerarReaproveitaCobranca() {
        pagamento.setMercadoPagoId("ORD1");
        pagamento.setMercadoPagoQr("000201pix");
        pagamento.setMercadoPagoQrBase64("QUJD");
        pagamento.setStatus(PagamentoStatus.AGUARDANDO_CONFIRMACAO);
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamento));

        PixResponse resposta = service.gerarPorAgendamento(10L);

        assertEquals("000201pix", resposta.pixCopiaECola());
        verify(mercadoPago, never()).criar(any(), any(), anyString(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o pagamento não é Pix")
    void gerarPagamentoPresencial() {
        pagamento.setMetodo(PagamentoMetodo.PRESENCIAL);
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamento));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.gerarPorAgendamento(10L));

        assertEquals("Pagamento não disponível para cobrança Pix.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o Pix já foi pago")
    void gerarPagamentoPago() {
        pagamento.setStatus(PagamentoStatus.PAGO);
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamento));

        assertThrows(BadRequestException.class, () -> service.gerarPorAgendamento(10L));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o pagamento não existe")
    void gerarSemPagamento() {
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.gerarPorAgendamento(10L));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException quando o Mercado Pago não devolve o QR Code")
    void gerarRespostaIncompleta() throws Exception {
        when(repository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamento));
        when(mercadoPago.criar(any(), any(), anyString(), anyString())).thenReturn(json("{\"id\":\"ORD1\"}"));

        assertThrows(IllegalStateException.class, () -> service.gerarPorAgendamento(10L));

        assertNull(pagamento.getMercadoPagoId());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve confirmar o pagamento quando a ordem está processada e creditada")
    void sincronizarConfirma() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json(ordemPaga("ORD1", "processed", "accredited", "pix", "100", "40.00")));

        service.sincronizar("ORD1");

        verify(pagamentoService).confirmarPixValidado("ORD1", new BigDecimal("40.00"), "100");
    }

    @Test
    @DisplayName("Não deve confirmar quando o id devolvido é diferente do consultado")
    void sincronizarIdDiferente() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json(ordemPaga("OUTRA", "processed", "accredited", "pix", "100", "40.00")));

        service.sincronizar("ORD1");

        verifyNoInteractions(pagamentoService);
    }

    @Test
    @DisplayName("Não deve confirmar quando a ordem ainda não foi processada")
    void sincronizarOrdemNaoProcessada() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json(ordemPaga("ORD1", "created", "accredited", "pix", "100", "40.00")));

        service.sincronizar("ORD1");

        verifyNoInteractions(pagamentoService);
    }

    @Test
    @DisplayName("Não deve confirmar quando o pagamento não está creditado")
    void sincronizarNaoCreditado() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json(ordemPaga("ORD1", "processed", "pending", "pix", "100", "40.00")));

        service.sincronizar("ORD1");

        verifyNoInteractions(pagamentoService);
    }

    @Test
    @DisplayName("Não deve confirmar quando o método de pagamento não é Pix")
    void sincronizarMetodoDiferente() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json(ordemPaga("ORD1", "processed", "accredited", "visa", "100", "40.00")));

        service.sincronizar("ORD1");

        verifyNoInteractions(pagamentoService);
    }

    @Test
    @DisplayName("Não deve confirmar quando a referência externa está vazia")
    void sincronizarSemReferencia() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json(ordemPaga("ORD1", "processed", "accredited", "pix", "", "40.00")));

        service.sincronizar("ORD1");

        verifyNoInteractions(pagamentoService);
    }

    @Test
    @DisplayName("Não deve confirmar quando o valor é inválido")
    void sincronizarValorInvalido() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json(ordemPaga("ORD1", "processed", "accredited", "pix", "100", "abc")));

        service.sincronizar("ORD1");

        verifyNoInteractions(pagamentoService);
    }

    @Test
    @DisplayName("Não deve confirmar quando o valor é zero")
    void sincronizarValorZero() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json(ordemPaga("ORD1", "processed", "accredited", "pix", "100", "0")));

        service.sincronizar("ORD1");

        verifyNoInteractions(pagamentoService);
    }

    @Test
    @DisplayName("Não deve confirmar quando a ordem não tem pagamentos")
    void sincronizarSemPagamentos() throws Exception {
        when(mercadoPago.consultar("ORD1")).thenReturn(json("{\"id\":\"ORD1\",\"status\":\"processed\",\"transactions\":{\"payments\":[]}}"));

        service.sincronizar("ORD1");

        verifyNoInteractions(pagamentoService);
    }
}
