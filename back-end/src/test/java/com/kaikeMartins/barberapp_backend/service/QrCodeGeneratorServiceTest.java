package com.kaikeMartins.barberapp_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class QrCodeGeneratorServiceTest {

    private final QrCodeGeneratorService service = new QrCodeGeneratorService();

    @Test
    @DisplayName("Deve gerar uma imagem PNG em Base64")
    void gerarBase64() {
        byte[] bytes = Base64.getDecoder().decode(service.gerarBase64("000201pix"));

        assertEquals((byte) 0x89, bytes[0]);
        assertEquals('P', bytes[1]);
        assertEquals('N', bytes[2]);
        assertEquals('G', bytes[3]);
    }

    @Test
    @DisplayName("Deve gerar imagens diferentes para conteúdos diferentes")
    void gerarImagensDiferentes() {
        assertNotEquals(service.gerarBase64("conteudo-um"), service.gerarBase64("conteudo-dois"));
    }
}
