package com.kaikeMartins.barberapp_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.PixResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PixService {

    private final PagamentoService pagamentoService;
    private final PagamentoRepository repository;
    private final MercadoPagoClient mercadoPago;

    @Value("${mercadopago.webhook-url}")
    private String webhookUrl;

    @Transactional
    public PixResponse gerarPorAgendamento(Long agendamentoId) {

        pagamentoService.consultar(agendamentoId);

        PagamentoEntity pagamento = repository.buscarParaAtualizacao(agendamentoId)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));

        if (pagamento.getMetodo() != PagamentoMetodo.PIX
                || pagamento.getStatus() == PagamentoStatus.PAGO
                || pagamento.getStatus() == PagamentoStatus.CANCELADO) {
            throw new BadRequestException(
                    "Pagamento não disponível para cobrança Pix."
            );
        }

        if (pagamento.getMercadoPagoId() == null) {

            String email = pagamento.getAgendamento()
                    .getCliente()
                    .getUsuario()
                    .getEmail();

            JsonNode order = mercadoPago.criar(
                    pagamento.getValor(),
                    pagamento.getId(),
                    email,
                    webhookUrl
            );

            String id = order.path("id").asText();

            JsonNode dados = order.path("transactions")
                    .path("payments")
                    .path(0)
                    .path("payment_method");

            String qr = dados.path("qr_code").asText();
            String base64 = dados.path("qr_code_base64").asText();

            if (id.isBlank() || qr.isBlank() || base64.isBlank()) {
                throw new IllegalStateException(
                        "Mercado Pago não retornou os dados da cobrança Pix."
                );
            }

            pagamento.setMercadoPagoId(id);
            pagamento.setMercadoPagoQr(qr);
            pagamento.setMercadoPagoQrBase64(base64);
            pagamento.setStatus(PagamentoStatus.AGUARDANDO_CONFIRMACAO);

            repository.save(pagamento);
        }

        return new PixResponse(
                agendamentoId,
                pagamento.getValor(),
                pagamento.getMercadoPagoQrBase64(),
                pagamento.getMercadoPagoQr()
        );
    }

    @Transactional
    public void sincronizar(String id) {

        JsonNode order = mercadoPago.consultar(id);

        if (!id.equals(order.path("id").asText())) {
            return;
        }

        if (!"processed".equalsIgnoreCase(order.path("status").asText())) {
            return;
        }

        JsonNode payments = order.path("transactions").path("payments");

        if (!payments.isArray() || payments.isEmpty()) {
            return;
        }

        JsonNode payment = payments.path(0);

        if (!"processed".equalsIgnoreCase(payment.path("status").asText())) {
            return;
        }

        if (!"accredited".equalsIgnoreCase(payment.path("status_detail").asText())) {
            return;
        }

        JsonNode metodo = payment.path("payment_method");

        if (!"pix".equalsIgnoreCase(metodo.path("id").asText())) {
            return;
        }

        String referencia = order.path("external_reference").asText();

        if (referencia.isBlank()) {
            return;
        }

        BigDecimal valor;

        try {
            valor = new BigDecimal(payment.path("amount").asText("0"));
        } catch (NumberFormatException e) {
            return;
        }

        if (valor.signum() <= 0) {
            return;
        }

        pagamentoService.confirmarPixValidado(
                id,
                valor,
                referencia
        );
    }
}
