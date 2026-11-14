package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.BarbeiroDesligadoResponse;
import com.kaikeMartins.barberapp_backend.dto.CancelamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.DesligarBarbeiroRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.GlobalExceptionHandler;
import com.kaikeMartins.barberapp_backend.service.BarbeiroService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BarbeiroControllerDesligamentoTest {

    @Mock
    private BarbeiroService service;

    @InjectMocks
    private BarbeiroController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /barbeiros/{id}/desligar deve retornar 200 com o resumo dos cancelamentos")
    void desligarRetorna200() throws Exception {
        when(service.desligar(3L, new DesligarBarbeiroRequest("Pedido de demissão")))
                .thenReturn(new CancelamentoResponse(2, 6));

        mockMvc.perform(post("/barbeiros/3/desligar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Pedido de demissão\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentosCancelados").value(2))
                .andExpect(jsonPath("$.horariosRemovidos").value(6));
    }

    @Test
    @DisplayName("POST /barbeiros/{id}/desligar deve retornar 400 quando o motivo está em branco")
    void desligarSemMotivo() throws Exception {
        mockMvc.perform(post("/barbeiros/3/desligar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).desligar(any(), any());
    }

    @Test
    @DisplayName("POST /barbeiros/{id}/desligar deve retornar 400 quando o motivo é curto demais")
    void desligarMotivoCurto() throws Exception {
        mockMvc.perform(post("/barbeiros/3/desligar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /barbeiros/{id}/desligar deve retornar 400 quando o barbeiro já foi desligado")
    void desligarJaDesligado() throws Exception {
        when(service.desligar(3L, new DesligarBarbeiroRequest("Outro motivo")))
                .thenThrow(new BadRequestException("Este barbeiro já foi desligado."));

        mockMvc.perform(post("/barbeiros/3/desligar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Outro motivo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Este barbeiro já foi desligado."));
    }

    @Test
    @DisplayName("GET /barbeiros/desligados deve retornar 200 e a lista com os motivos")
    void desligadosRetorna200() throws Exception {
        when(service.listarDesligados()).thenReturn(List.of(
                new BarbeiroDesligadoResponse(3L, "Joao", "joao@email.com", LocalDateTime.of(2026, 11, 1, 10, 0), "Faltas")));

        mockMvc.perform(get("/barbeiros/desligados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Joao"))
                .andExpect(jsonPath("$[0].motivo").value("Faltas"));
    }
}
