package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.dto.PagamentoResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.GlobalExceptionHandler;
import com.kaikeMartins.barberapp_backend.service.PagamentoService;
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

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagamentoControllerTest {

    @Mock
    private PagamentoService service;

    @InjectMocks
    private PagamentoController controller;

    private MockMvc mockMvc;

    private PagamentoResponse resposta;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        resposta = new PagamentoResponse(100L, 10L, new BigDecimal("40.00"), "PIX", "PENDENTE", null);
    }

    @Test
    @DisplayName("GET /pagamentos/agendamentos/{id} deve retornar 200 e o pagamento")
    void consultarRetorna200() throws Exception {
        when(service.consultar(10L)).thenReturn(resposta);

        mockMvc.perform(get("/pagamentos/agendamentos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentoId").value(10))
                .andExpect(jsonPath("$.metodo").value("PIX"));
    }

    @Test
    @DisplayName("PUT /pagamentos/agendamentos/{id}/metodo deve escolher o método informado")
    void escolherMetodoRetorna200() throws Exception {
        when(service.escolherMetodo(10L, PagamentoMetodo.PIX)).thenReturn(resposta);

        mockMvc.perform(put("/pagamentos/agendamentos/10/metodo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"PIX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST /pagamentos/agendamentos/{id}/confirmar deve retornar 200 quando confirmado")
    void confirmarRetorna200() throws Exception {
        when(service.confirmar(10L)).thenReturn(
                new PagamentoResponse(101L, 10L, new BigDecimal("40.00"), "PRESENCIAL", "PAGO", null));

        mockMvc.perform(post("/pagamentos/agendamentos/10/confirmar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGO"));
    }

    @Test
    @DisplayName("POST /pagamentos/agendamentos/{id}/informar-pix deve retornar 400")
    void informarPixRejeitado() throws Exception {
        when(service.informarPix(10L))
                .thenThrow(new BadRequestException("O Pix é confirmado automaticamente pelo Mercado Pago."));

        mockMvc.perform(post("/pagamentos/agendamentos/10/informar-pix"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O Pix é confirmado automaticamente pelo Mercado Pago."));
    }
}
