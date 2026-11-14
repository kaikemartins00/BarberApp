package com.kaikeMartins.barberapp_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoWebhookSignatureServiceTest {

    private static final String SEGREDO = "segredo-de-teste";
    private static final String REQUEST_ID = "req-123";
    private static final String ID = "ORD01ABC";

    private MercadoPagoWebhookSignatureService service;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoWebhookSignatureService();
        ReflectionTestUtils.setField(service, "segredo", SEGREDO);
    }

    private String assinar(String id, String requestId, String timestamp, String segredo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String manifesto = "id:" + id.toLowerCase() + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        return HexFormat.of().formatHex(mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8)));
    }

    private String cabecalho(String timestamp, String v1) {
        return "ts=" + timestamp + ",v1=" + v1;
    }

    @Test
    @DisplayName("Deve aceitar assinatura válida com timestamp em segundos")
    void assinaturaValidaEmSegundos() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);

        assertTrue(service.validar(cabecalho(ts, assinar(ID, REQUEST_ID, ts, SEGREDO)), REQUEST_ID, ID));
    }

    @Test
    @DisplayName("Deve aceitar assinatura válida com timestamp em milissegundos")
    void assinaturaValidaEmMilissegundos() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis());

        assertTrue(service.validar(cabecalho(ts, assinar(ID, REQUEST_ID, ts, SEGREDO)), REQUEST_ID, ID));
    }

    @Test
    @DisplayName("Deve rejeitar assinatura calculada com outro segredo")
    void segredoErrado() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);

        assertFalse(service.validar(cabecalho(ts, assinar(ID, REQUEST_ID, ts, "outro-segredo")), REQUEST_ID, ID));
    }

    @Test
    @DisplayName("Deve rejeitar assinatura de outro id")
    void idDiferente() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);

        assertFalse(service.validar(cabecalho(ts, assinar("OUTRO", REQUEST_ID, ts, SEGREDO)), REQUEST_ID, ID));
    }

    @Test
    @DisplayName("Deve rejeitar assinatura com mais de cinco minutos")
    void assinaturaExpirada() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() / 1000 - 600);

        assertFalse(service.validar(cabecalho(ts, assinar(ID, REQUEST_ID, ts, SEGREDO)), REQUEST_ID, ID));
    }

    @Test
    @DisplayName("Deve rejeitar quando faltam cabeçalhos ou parâmetros")
    void parametrosAusentes() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String valida = cabecalho(ts, assinar(ID, REQUEST_ID, ts, SEGREDO));

        assertFalse(service.validar(null, REQUEST_ID, ID));
        assertFalse(service.validar(valida, null, ID));
        assertFalse(service.validar(valida, REQUEST_ID, null));
    }

    @Test
    @DisplayName("Deve rejeitar assinatura mal formada")
    void assinaturaMalFormada() {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);

        assertFalse(service.validar("sem-formato", REQUEST_ID, ID));
        assertFalse(service.validar(cabecalho(ts, "curta"), REQUEST_ID, ID));
        assertFalse(service.validar(cabecalho("abc", "a".repeat(64)), REQUEST_ID, ID));
        assertFalse(service.validar("ts=" + ts, REQUEST_ID, ID));
    }

    @Test
    @DisplayName("Deve rejeitar tudo quando o segredo não está configurado")
    void semSegredo() throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String assinatura = cabecalho(ts, assinar(ID, REQUEST_ID, ts, SEGREDO));
        ReflectionTestUtils.setField(service, "segredo", " ");

        assertFalse(service.validar(assinatura, REQUEST_ID, ID));
    }
}
