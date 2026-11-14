package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.kaikeMartins.barberapp_backend.service.MercadoPagoWebhookService;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookControllerTest {

    @Mock
    private MercadoPagoWebhookService webhookService;

    @InjectMocks
    private MercadoPagoWebhookController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST deve repassar corpo, cabeçalhos e parâmetros ao serviço e retornar 200")
    void receberCompleto() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago")
                        .param("data.id", "ORD1")
                        .param("type", "order")
                        .header("x-signature", "ts=1,v1=abc")
                        .header("x-request-id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"order.processed\"}"))
                .andExpect(status().isOk());

        verify(webhookService).processar(anyMap(), eq("ts=1,v1=abc"), eq("req-1"), eq("ORD1"), eq("order"));
    }

    @Test
    @DisplayName("POST deve aceitar chamada sem corpo, cabeçalhos ou parâmetros")
    void receberVazio() throws Exception {
        mockMvc.perform(post("/webhooks/mercadopago"))
                .andExpect(status().isOk());

        verify(webhookService).processar(isNull(), isNull(), isNull(), isNull(), isNull());
    }
}
