package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.service.MercadoPagoWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhooks/mercadopago")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final MercadoPagoWebhookService webhookService;

    @PostMapping
    public ResponseEntity<Void> receber(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(name = "x-signature", required = false) String assinatura,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestParam(name = "data.id", required = false) String queryId,
            @RequestParam(name = "type", required = false) String type
    ) {
        webhookService.processar(body, assinatura, requestId, queryId, type);

        return ResponseEntity.ok().build();
    }
}