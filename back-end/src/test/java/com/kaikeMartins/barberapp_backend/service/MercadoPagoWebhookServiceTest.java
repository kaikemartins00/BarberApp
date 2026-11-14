package com.kaikeMartins.barberapp_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookServiceTest {

    @InjectMocks
    private MercadoPagoWebhookService service;

    @Mock
    private MercadoPagoWebhookSignatureService signatureService;

    @Mock
    private PixService pixService;

    @Test
    @DisplayName("Deve sincronizar o Pix quando a assinatura é válida")
    void processarSuccess() {
        when(signatureService.validar("assinatura", "req-1", "123")).thenReturn(true);

        service.processar(Map.of("type", "order", "data", Map.of("id", 123)), "assinatura", "req-1", null, null);

        verify(pixService).sincronizar("123");
    }

    @Test
    @DisplayName("Deve preferir o id e o tipo informados na query")
    void processarUsaIdETipoDaQuery() {
        when(signatureService.validar("assinatura", "req-1", "999")).thenReturn(true);

        service.processar(Map.of("type", "payment", "data", Map.of("id", 123)), "assinatura", "req-1", " 999 ", "order");

        verify(pixService).sincronizar("999");
    }

    @Test
    @DisplayName("Deve lançar SecurityException quando a assinatura é inválida")
    void processarAssinaturaInvalida() {
        when(signatureService.validar("falsa", "req-1", "123")).thenReturn(false);

        SecurityException e = assertThrows(SecurityException.class,
                () -> service.processar(Map.of("type", "order", "data", Map.of("id", 123)), "falsa", "req-1", null, null));

        assertEquals("Assinatura do webhook do Mercado Pago inválida.", e.getMessage());
        verifyNoInteractions(pixService);
    }

    @Test
    @DisplayName("Deve ignorar notificações de outros tipos")
    void processarIgnoraOutroTipo() {
        service.processar(Map.of("type", "payment", "data", Map.of("id", 123)), "assinatura", "req-1", null, null);

        verifyNoInteractions(signatureService, pixService);
    }

    @Test
    @DisplayName("Deve ignorar notificações sem id")
    void processarIgnoraSemId() {
        service.processar(Map.of("type", "order"), "assinatura", "req-1", null, null);

        verifyNoInteractions(signatureService, pixService);
    }

    @Test
    @DisplayName("Deve ignorar notificações com corpo nulo e sem query")
    void processarCorpoNulo() {
        service.processar(null, "assinatura", "req-1", null, null);

        verifyNoInteractions(signatureService, pixService);
    }
}
