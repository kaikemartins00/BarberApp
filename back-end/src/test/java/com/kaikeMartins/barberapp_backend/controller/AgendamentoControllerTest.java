package com.kaikeMartins.barberapp_backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.service.AgendamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgendamentoControllerTest {

    @Mock
    private AgendamentoService service;

    @InjectMocks
    private AgendamentoController controller;

    private AgendamentoRequest agendamentoRequest;

    private AgendamentoResponse agendamentoResponse;

    private MockMvc mockMvc;

    private String jsonRequest;

    private String url;

    private LocalDate amanha;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).alwaysDo(print()).build();
        url = "/agendamentos";
        amanha = LocalDate.now().plusDays(1);
        agendamentoRequest = new AgendamentoRequest(2L, 3L, amanha, LocalTime.of(14, 0));
        agendamentoResponse = new AgendamentoResponse(10L, "Kaike", "Joao", "Corte", 40.0, amanha,
                LocalTime.of(14, 0), "PENDENTE");
        jsonRequest = objectMapper.writeValueAsString(agendamentoRequest);
    }

    @Test
    @DisplayName("GET /agendamentos/meus deve retornar 200 e a lista do usuário")
    void meusAgendamentos_deveRetornar200() throws Exception {

        when(service.meusAgendamentos()).thenReturn(List.of(agendamentoResponse));

        ResultActions result = mockMvc.perform(get(url + "/meus"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].clienteNome").value("Kaike"))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));

        verify(service).meusAgendamentos();
    }

    @Test
    @DisplayName("POST /agendamentos deve retornar 201 e delegar ao service")
    void create_deveRetornar201() throws Exception {

        when(service.create(agendamentoRequest)).thenReturn(agendamentoResponse);

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.servicoNome").value("Corte"))
                .andExpect(jsonPath("$.servicoPreco").value(40.0));

        verify(service).create(agendamentoRequest);
    }

    @Test
    @DisplayName("POST /agendamentos sem corpo deve retornar 400")
    void create_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /agendamentos com campos nulos deve retornar 400")
    void create_camposNulos_deveRetornar400() throws Exception {

        String jsonVazio = "{}";

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonVazio));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /agendamentos com data de hoje (não futura) deve retornar 400")
    void create_dataNaoFutura_deveRetornar400() throws Exception {

        AgendamentoRequest hoje = new AgendamentoRequest(2L, 3L, LocalDate.now(), LocalTime.of(14, 0));
        String jsonHoje = objectMapper.writeValueAsString(hoje);

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonHoje));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET /agendamentos/horarios-livres deve retornar os horários do barbeiro na data")
    void horariosLivres_deveRetornar200() throws Exception {

        when(service.horariosLivres(2L, amanha))
                .thenReturn(List.of(LocalTime.of(9, 0), LocalTime.of(10, 0)));

        ResultActions result = mockMvc.perform(get(url + "/horarios-livres")
                .param("barbeiroId", "2")
                .param("data", amanha.toString()));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(service).horariosLivres(2L, amanha);
    }

    @Test
    @DisplayName("GET /agendamentos/horarios-livres sem parâmetros obrigatórios deve retornar 400")
    void horariosLivres_semParametros_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(get(url + "/horarios-livres"));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET /agendamentos/horarios-livres com data inválida deve retornar 400")
    void horariosLivres_dataInvalida_deveRetornar400() throws Exception {

        String dataInvalida = "10/11/2026";

        ResultActions result = mockMvc.perform(get(url + "/horarios-livres")
                .param("barbeiroId", "2")
                .param("data", dataInvalida));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("GET /agendamentos deve retornar 200 e todos os agendamentos")
    void findAll_deveRetornar200() throws Exception {

        when(service.findAll()).thenReturn(List.of(agendamentoResponse, agendamentoResponse));

        ResultActions result = mockMvc.perform(get(url));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(service).findAll();
    }

    @Test
    @DisplayName("PUT /agendamentos/{id}/status deve atualizar o status")
    void atualizarStatus_deveRetornar200() throws Exception {

        AgendamentoResponse confirmado = new AgendamentoResponse(10L, "Kaike", "Joao", "Corte", 40.0, amanha,
                LocalTime.of(14, 0), "CONFIRMADO");
        when(service.atualizarStatus(10L, AgendamentoStatus.CONFIRMADO)).thenReturn(confirmado);

        ResultActions result = mockMvc.perform(put(url + "/10/status")
                .param("status", "CONFIRMADO"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        verify(service).atualizarStatus(10L, AgendamentoStatus.CONFIRMADO);
    }

    @Test
    @DisplayName("PUT /agendamentos/{id}/status com status inexistente deve retornar 400")
    void atualizarStatus_statusInvalido_deveRetornar400() throws Exception {

        String statusInvalido = "STATUS_QUE_NAO_EXISTE";


        ResultActions result = mockMvc.perform(put(url + "/10/status")
                .param("status", statusInvalido));


        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}