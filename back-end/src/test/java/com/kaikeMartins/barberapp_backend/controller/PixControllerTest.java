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
import com.kaikeMartins.barberapp_backend.dto.PixResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.service.PixService;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PixControllerTest {

    @Mock
    private PixService pixService;

    @InjectMocks
    private PixController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /pagamentos/pix/agendamento/{id} deve retornar o QR Code")
    void gerar() throws Exception {
        when(pixService.gerarPorAgendamento(10L))
                .thenReturn(new PixResponse(10L, new BigDecimal("40.00"), "QUJD", "000201pix"));

        mockMvc.perform(get("/pagamentos/pix/agendamento/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentoId").value(10))
                .andExpect(jsonPath("$.qrCodeBase64").value("QUJD"))
                .andExpect(jsonPath("$.pixCopiaECola").value("000201pix"));
    }

    @Test
    @DisplayName("GET deve retornar 400 quando o pagamento não permite Pix")
    void naoDisponivel() throws Exception {
        when(pixService.gerarPorAgendamento(10L))
                .thenThrow(new BadRequestException("Pagamento não disponível para cobrança Pix."));

        mockMvc.perform(get("/pagamentos/pix/agendamento/10"))
                .andExpect(status().isBadRequest());
    }
}
