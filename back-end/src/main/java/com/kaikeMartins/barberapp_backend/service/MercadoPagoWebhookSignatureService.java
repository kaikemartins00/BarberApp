package com.kaikeMartins.barberapp_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class MercadoPagoWebhookSignatureService {

    @Value("${mercadopago.webhook-secret}")
    private String segredo;

    public boolean validar(String assinatura, String requestId, String id) {
        if (assinatura == null || requestId == null || id == null ||
                segredo == null || segredo.isBlank()) {
            return false;
        }

        String timestamp = null;
        String assinaturaRecebida = null;

        for (String parte : assinatura.split(",")) {
            String[] campos = parte.trim().split("=", 2);

            if (campos.length != 2) {
                continue;
            }

            if ("ts".equals(campos[0])) {
                timestamp = campos[1];
            }

            if ("v1".equals(campos[0])) {
                assinaturaRecebida = campos[1];
            }
        }

        if (timestamp == null || assinaturaRecebida == null ||
                !timestamp.matches("[0-9]+") ||
                !assinaturaRecebida.matches("[0-9a-fA-F]{64}")) {
            return false;
        }

        try {
            long instante = Long.parseLong(timestamp);

            if (timestamp.length() <= 11) {
                instante = instante * 1000L;
            }

            if (Math.abs(System.currentTimeMillis() - instante) > 300000L) {
                return false;
            }

            String manifesto = "id:" + id.toLowerCase() +
                    ";request-id:" + requestId +
                    ";ts:" + timestamp + ";";

            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(
                    segredo.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] esperado = mac.doFinal(
                    manifesto.getBytes(StandardCharsets.UTF_8)
            );

            byte[] recebido = HexFormat.of().parseHex(assinaturaRecebida);

            return MessageDigest.isEqual(esperado, recebido);

        } catch (Exception e) {
            return false;
        }
    }
}