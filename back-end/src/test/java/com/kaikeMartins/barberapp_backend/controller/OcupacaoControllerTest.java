package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.OcupacaoResponse;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.GlobalExceptionHandler;
import com.kaikeMartins.barberapp_backend.service.OcupacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OcupacaoControllerTest {

    @Mock
    private OcupacaoService service;

    @InjectMocks
    private OcupacaoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /ocupacao/barbeiros/{id} deve retornar 200 e a ocupação do dia")
    void consultarRetorna200() throws Exception {
        LocalDate dia = LocalDate.of(2026, 11, 20);
        when(service.consultar(2L, dia)).thenReturn(new OcupacaoResponse(2L, dia, 4, 1, 3, 25.0));

        mockMvc.perform(get("/ocupacao/barbeiros/2").param("data", "2026-11-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horariosDisponibilizados").value(4))
                .andExpect(jsonPath("$.horariosLivres").value(3))
                .andExpect(jsonPath("$.percentualOcupacao").value(25.0));
    }

    @Test
    @DisplayName("GET /ocupacao/barbeiros/{id} deve retornar 403 quando o acesso é negado")
    void consultarSemPermissao() throws Exception {
        LocalDate dia = LocalDate.of(2026, 11, 20);
        when(service.consultar(2L, dia)).thenThrow(new ForbiddenException("Você não pode consultar a ocupação de outro barbeiro"));

        mockMvc.perform(get("/ocupacao/barbeiros/2").param("data", "2026-11-20"))
                .andExpect(status().isForbidden());
    }
}
