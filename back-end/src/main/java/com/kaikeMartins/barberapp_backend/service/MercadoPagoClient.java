package com.kaikeMartins.barberapp_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class MercadoPagoClient {

    private final RestClient client;
    private final ObjectMapper mapper;

    public MercadoPagoClient(
            @Value("${mercadopago.access-token}") String token
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Configure MERCADOPAGO_ACCESS_TOKEN."
            );
        }

        this.mapper = new ObjectMapper();

        this.client = RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    private static String detalhe(Exception e) {
        if (e instanceof RestClientResponseException r) {
            return " (HTTP " + r.getStatusCode().value() + ": " + r.getResponseBodyAsString() + ")";
        }
        return "";
    }

    public JsonNode criar(
            BigDecimal valor,
            Long referencia,
            String email,
            String webhookUrl
    ) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    "O valor da cobrança Pix deve ser maior que zero."
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "O e-mail do cliente é obrigatório."
            );
        }

        try {
            String valorFormatado = valor.setScale(2)
                    .toPlainString();

            Map<String, Object> pagamento = Map.of(
                    "amount", valorFormatado,
                    "payment_method", Map.of(
                            "id", "pix",
                            "type", "bank_transfer"
                    )
            );

            Map<String, Object> body = Map.of(
                    "type", "online",
                    "processing_mode", "automatic",
                    "total_amount", valorFormatado,
                    "external_reference", String.valueOf(referencia),
                    "payer", Map.of("email", email),
                    "transactions", Map.of(
                            "payments", new Object[]{pagamento}
                    )
            );

            String response = client.post()
                    .uri("/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                            "X-Idempotency-Key",
                            "barberapp-order-" + referencia
                    )
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return mapper.readTree(response);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha ao criar Order Pix no Mercado Pago." + detalhe(e),
                    e
            );
        }
    }

    public JsonNode reembolsar(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Identificador da Order inválido."
            );
        }

        try {
            String response = client.post()
                    .uri("/v1/orders/{id}/refund", id)
                    .header("X-Idempotency-Key", "barberapp-refund-" + id)
                    .retrieve()
                    .body(String.class);

            return mapper.readTree(
                    response == null || response.isBlank() ? "{}" : response
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha ao reembolsar Order Pix no Mercado Pago." + detalhe(e),
                    e
            );
        }
    }

    public JsonNode consultar(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Identificador da Order inválido."
            );
        }

        try {
            String response = client.get()
                    .uri("/v1/orders/{id}", id)
                    .retrieve()
                    .body(String.class);

            return mapper.readTree(response);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha ao consultar Order Pix no Mercado Pago." + detalhe(e),
                    e
            );
        }
    }
}
