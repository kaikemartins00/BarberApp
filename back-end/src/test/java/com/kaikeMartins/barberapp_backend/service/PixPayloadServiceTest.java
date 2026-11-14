package com.kaikeMartins.barberapp_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PixPayloadServiceTest {

    private PixPayloadService service;

    @BeforeEach
    void setUp() {
        service = new PixPayloadService("chave@email.com", "José da Silva", "Aracaju");
    }

    private String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    @Test
    @DisplayName("Deve gerar o payload Pix com os campos obrigatórios")
    void gerarCamposObrigatorios() {
        String payload = service.gerar(new BigDecimal("40"), 100L);

        assertTrue(payload.startsWith("000201"));
        assertTrue(payload.contains("br.gov.bcb.pix"));
        assertTrue(payload.contains("chave@email.com"));
        assertTrue(payload.contains("540540.00"));
        assertTrue(payload.contains("5303986"));
        assertTrue(payload.contains("5802BR"));
        assertTrue(payload.contains("0509BARBER100"));
    }

    @Test
    @DisplayName("Deve normalizar nome do recebedor e cidade sem acentos e em maiúsculas")
    void gerarNormalizaTextos() {
        String payload = service.gerar(new BigDecimal("10.00"), 1L);

        assertTrue(payload.contains("5913JOSE DA SILVA"));
        assertTrue(payload.contains("6007ARACAJU"));
    }

    @Test
    @DisplayName("Deve terminar com o CRC16 correto do restante do payload")
    void gerarCrcCorreto() {
        String payload = service.gerar(new BigDecimal("25.5"), 7L);

        String semCrc = payload.substring(0, payload.length() - 4);

        assertTrue(semCrc.endsWith("6304"));
        assertEquals(crc16(semCrc), payload.substring(payload.length() - 4));
    }

    @Test
    @DisplayName("Deve arredondar o valor para duas casas decimais")
    void gerarArredondaValor() {
        assertTrue(service.gerar(new BigDecimal("10.005"), 1L).contains("540510.01"));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o valor é nulo, zero ou negativo")
    void gerarValorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.gerar(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> service.gerar(BigDecimal.ZERO, 1L));
        assertThrows(IllegalArgumentException.class, () -> service.gerar(new BigDecimal("-1"), 1L));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o id do pagamento é inválido")
    void gerarIdInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.gerar(BigDecimal.TEN, null));
        assertThrows(IllegalArgumentException.class, () -> service.gerar(BigDecimal.TEN, 0L));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando a configuração Pix está incompleta")
    void configuracaoIncompleta() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new PixPayloadService(" ", "Recebedor", "Cidade"));

        assertEquals("Configuração Pix incompleta.", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new PixPayloadService("chave", " ", "Cidade"));
        assertThrows(IllegalArgumentException.class, () -> new PixPayloadService("chave", "Recebedor", " "));
    }

    @Test
    @DisplayName("Deve limitar o nome do recebedor a 25 caracteres e a cidade a 15")
    void limitaTamanhos() {
        PixPayloadService longo = new PixPayloadService("chave", "A".repeat(40), "B".repeat(30));

        String payload = longo.gerar(BigDecimal.TEN, 1L);

        assertTrue(payload.contains("5925" + "A".repeat(25)));
        assertTrue(payload.contains("6015" + "B".repeat(15)));
    }
}
