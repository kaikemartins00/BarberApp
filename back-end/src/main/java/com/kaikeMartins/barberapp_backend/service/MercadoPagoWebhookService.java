package com.kaikeMartins.barberapp_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPagoWebhookService {

    private final MercadoPagoWebhookSignatureService signatureService;
    private final PixService pixService;

    public void processar(
            Map<String, Object> body,
            String assinatura,
            String requestId,
            String queryId,
            String type
    ) {
        String id = extrairId(body, queryId);
        String tipo = extrairTipo(body, type);

        if (id == null || id.isBlank()) {
            return;
        }

        if (!"order".equalsIgnoreCase(tipo)) {
            return;
        }

        if (!signatureService.validar(assinatura, requestId, id)) {
            throw new SecurityException(
                    "Assinatura do webhook do Mercado Pago inválida."
            );
        }

        pixService.sincronizar(id);
    }

    private String extrairId(Map<String, Object> body, String queryId) {

        if (queryId != null && !queryId.isBlank()) {
            return queryId.trim();
        }

        if (body == null) {
            return null;
        }

        Object data = body.get("data");

        if (data instanceof Map<?, ?> dados) {
            Object id = dados.get("id");

            if (id != null) {
                return String.valueOf(id).trim();
            }
        }

        return null;
    }

    private String extrairTipo(Map<String, Object> body, String type) {

        if (type != null && !type.isBlank()) {
            return type.trim();
        }

        if (body == null) {
            return null;
        }

        Object tipo = body.get("type");

        if (tipo != null) {
            return String.valueOf(tipo).trim();
        }

        return null;
    }
}
