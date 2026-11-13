
package com.kaikeMartins.barberapp_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

@Service
public class PixPayloadService {

    private final String chave;
    private final String recebedor;
    private final String cidade;

    public PixPayloadService(@Value("${pix.chave}") String chave, @Value("${pix.recebedor}") String recebedor, @Value("${pix.cidade}") String cidade) {
        this.chave = chave.trim();
        this.recebedor = normalizar(recebedor, 25);
        this.cidade = normalizar(cidade, 15);

        if (this.chave.isBlank()
                || this.recebedor.isBlank()
                || this.cidade.isBlank()) {
            throw new IllegalArgumentException(
                    "Configuração Pix incompleta."
            );
        }
    }

    public String gerar(BigDecimal valor, Long pagamentoId) {

        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    "O valor do Pix deve ser maior que zero."
            );
        }

        if (pagamentoId == null || pagamentoId <= 0) {
            throw new IllegalArgumentException(
                    "Identificador de pagamento inválido."
            );
        }

        String txid = "BARBER" + pagamentoId;

        if (txid.length() > 25) {
            throw new IllegalArgumentException(
                    "Identificador Pix muito longo."
            );
        }

        String contaPix = campo("00", "br.gov.bcb.pix") +
                        campo("01", chave);

        String payload =
                campo("00", "01") +
                        campo("26", contaPix) +
                        campo("52", "0000") +
                        campo("53", "986") +
                        campo("54", valor.setScale(
                                2,
                                java.math.RoundingMode.HALF_UP
                        ).toPlainString()) +
                        campo("58", "BR") +
                        campo("59", recebedor) +
                        campo("60", cidade) +
                        campo("62", campo("05", txid)) +
                        "6304";

        return payload + crc16(payload);
    }

    private String campo(String id, String valor) {
        int tamanho = valor.getBytes(
                StandardCharsets.UTF_8
        ).length;

        if (tamanho > 99) {
            throw new IllegalArgumentException(
                    "Campo Pix excede o tamanho permitido: " + id
            );
        }

        return id +
                String.format(Locale.ROOT, "%02d", tamanho) +
                valor;
    }

    private String normalizar(String texto, int limite) {

        String normalizado = Normalizer.normalize(
                texto.trim(),
                Normalizer.Form.NFD
        );

        normalizado = normalizado
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .trim()
                .replaceAll(" +", " ");

        return normalizado.length() > limite
                ? normalizado.substring(0, limite).trim()
                : normalizado;
    }

    private String crc16(String payload) {

        int crc = 0xFFFF;

        for (byte b : payload.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }

                crc &= 0xFFFF;
            }
        }

        return String.format(Locale.ROOT, "%04X", crc);
    }
}
