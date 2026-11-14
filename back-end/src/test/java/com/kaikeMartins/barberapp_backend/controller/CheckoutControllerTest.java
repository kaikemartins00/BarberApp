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
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.CheckoutRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.service.CheckoutService;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    @Mock
    private CheckoutService checkoutService;

    @InjectMocks
    private CheckoutController controller;

    private MockMvc mockMvc;

    private String corpo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        corpo = "{\"agendamento\":{\"barbeiroId\":2,\"servicoId\":3,\"data\":\"" + LocalDate.now().plusDays(1)
                + "\",\"horario\":\"14:00:00\"},\"metodo\":\"PIX\"}";
    }

    @Test
    @DisplayName("POST /agendamentos/checkout deve retornar 201 e o agendamento")
    void finalizar() throws Exception {
        when(checkoutService.finalizar(any(CheckoutRequest.class))).thenReturn(new AgendamentoResponse(
                10L, "Cliente", "Barbeiro", "Corte", 40.0, LocalDate.now().plusDays(1), LocalTime.of(14, 0), "PENDENTE"));

        mockMvc.perform(post("/agendamentos/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST deve retornar 400 quando o método de pagamento não é informado")
    void semMetodo() throws Exception {
        mockMvc.perform(post("/agendamentos/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamento\":{\"barbeiroId\":2,\"servicoId\":3,\"data\":\"" + LocalDate.now().plusDays(1)
                                + "\",\"horario\":\"14:00:00\"}}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(checkoutService);
    }

    @Test
    @DisplayName("POST deve retornar 400 quando o agendamento não é informado")
    void semAgendamento() throws Exception {
        mockMvc.perform(post("/agendamentos/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"PIX\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(checkoutService);
    }

    @Test
    @DisplayName("POST deve retornar 400 quando o serviço rejeita o horário")
    void horarioIndisponivel() throws Exception {
        when(checkoutService.finalizar(any(CheckoutRequest.class)))
                .thenThrow(new BadRequestException("Horário indisponível."));

        mockMvc.perform(post("/agendamentos/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest());
    }
}
