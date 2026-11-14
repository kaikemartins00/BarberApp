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
import com.kaikeMartins.barberapp_backend.dto.NotaAgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.PreferenciasClienteRequest;
import com.kaikeMartins.barberapp_backend.dto.PreferenciasClienteResponse;
import com.kaikeMartins.barberapp_backend.dto.ServicoPopularResponse;
import com.kaikeMartins.barberapp_backend.service.PainelClienteService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PainelClienteControllerTest {

    @Mock
    private PainelClienteService service;

    @InjectMocks
    private PainelClienteController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /cliente/painel/avaliacoes deve listar as notas do cliente")
    void avaliacoes() throws Exception {
        when(service.notasDoCliente()).thenReturn(List.of(new NotaAgendamentoResponse(10L, 5)));

        mockMvc.perform(get("/cliente/painel/avaliacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agendamentoId").value(10))
                .andExpect(jsonPath("$[0].nota").value(5));
    }

    @Test
    @DisplayName("GET /cliente/painel/servicos-populares deve listar os serviços mais feitos")
    void servicosPopulares() throws Exception {
        when(service.servicosMaisFeitos()).thenReturn(List.of(new ServicoPopularResponse("Corte", 4L)));

        mockMvc.perform(get("/cliente/painel/servicos-populares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].servico").value("Corte"))
                .andExpect(jsonPath("$[0].quantidade").value(4));
    }

    @Test
    @DisplayName("GET /cliente/painel/preferencias deve retornar as preferências")
    void preferencias() throws Exception {
        when(service.preferencias()).thenReturn(new PreferenciasClienteResponse("Máquina 2"));

        mockMvc.perform(get("/cliente/painel/preferencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").value("Máquina 2"));
    }

    @Test
    @DisplayName("PUT /cliente/painel/preferencias deve salvar e retornar as preferências")
    void salvarPreferencias() throws Exception {
        when(service.salvarPreferencias(any(PreferenciasClienteRequest.class)))
                .thenReturn(new PreferenciasClienteResponse("Tesoura"));

        mockMvc.perform(put("/cliente/painel/preferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacoes\":\"Tesoura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").value("Tesoura"));
    }

    @Test
    @DisplayName("PUT deve retornar 400 quando as preferências passam de 2000 caracteres")
    void salvarPreferenciasLongas() throws Exception {
        mockMvc.perform(put("/cliente/painel/preferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacoes\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
