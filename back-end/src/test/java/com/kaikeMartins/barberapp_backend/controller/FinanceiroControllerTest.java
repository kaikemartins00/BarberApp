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
import com.kaikeMartins.barberapp_backend.dto.FinanceiroResumoResponse;
import com.kaikeMartins.barberapp_backend.service.FinanceiroService;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceiroControllerTest {

    @Mock
    private FinanceiroService service;

    @InjectMocks
    private FinanceiroController controller;

    private MockMvc mockMvc;

    private FinanceiroResumoResponse resumo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        resumo = new FinanceiroResumoResponse(new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"), 3L);
    }

    @Test
    @DisplayName("GET /financeiro/barbeiro/meu deve retornar o resumo do barbeiro")
    void meu() throws Exception {
        when(service.meu()).thenReturn(resumo);

        mockMvc.perform(get("/financeiro/barbeiro/meu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBruto").value(100.00))
                .andExpect(jsonPath("$.taxaLoja").value(10.00))
                .andExpect(jsonPath("$.totalLiquido").value(90.00))
                .andExpect(jsonPath("$.quantidadePagamentos").value(3));
    }

    @Test
    @DisplayName("GET /financeiro/admin/resumo deve retornar o resumo geral")
    void geral() throws Exception {
        when(service.geral()).thenReturn(resumo);

        mockMvc.perform(get("/financeiro/admin/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLiquido").value(90.00));
    }
}
