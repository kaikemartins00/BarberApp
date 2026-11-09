package com.kaikeMartins.barberapp_backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;
import com.kaikeMartins.barberapp_backend.dto.ChamadoRequest;
import com.kaikeMartins.barberapp_backend.dto.ChamadoResponse;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamRequest;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamResponse;
import com.kaikeMartins.barberapp_backend.service.ChamadoService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChamadoControllerTest {

    @Mock
    private ChamadoService service;

    @InjectMocks
    private ChamadoController controller;

    private ChamadoRequest chamadoRequest;

    private RespostaChamRequest respostaRequest;

    private ChamadoResponse chamadoResponse;

    private MockMvc mockMvc;

    private String jsonChamado;

    private String jsonResposta;

    private String url;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).alwaysDo(print()).build();
        url = "/chamados";
        chamadoRequest = new ChamadoRequest("Duvida", "Como agendar?");
        respostaRequest = new RespostaChamRequest("Vou verificar");

        RespostaChamResponse resposta = new RespostaChamResponse(1L, "Vou verificar", LocalDateTime.now(), "Joao");
        chamadoResponse = new ChamadoResponse(7L, "Duvida", "Como agendar?", LocalDateTime.now(),
                ChamadoStatus.EM_ANDAMENTO, PrioridadeChamado.BAIXA, "Kaike", "kaike@email.com",
                "Joao", true, List.of(resposta));

        jsonChamado = objectMapper.writeValueAsString(chamadoRequest);
        jsonResposta = objectMapper.writeValueAsString(respostaRequest);
    }

    @Test
    @DisplayName("GET /chamados deve retornar 200 e todos os chamados")
    void findAll_deveRetornar200() throws Exception {

        when(service.findAll()).thenReturn(List.of(chamadoResponse));

        ResultActions result = mockMvc.perform(get(url));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Duvida"))
                .andExpect(jsonPath("$[0].status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$[0].respostas[0].autorNome").value("Joao"));

        verify(service).findAll();
    }

    @Test
    @DisplayName("GET /chamados/meus deve retornar 200 e os chamados do cliente")
    void meus_deveRetornar200() throws Exception {

        when(service.meus()).thenReturn(List.of(chamadoResponse));

        ResultActions result = mockMvc.perform(get(url + "/meus"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clienteEmail").value("kaike@email.com"));

        verify(service).meus();
    }

    @Test
    @DisplayName("POST /chamados deve retornar 201")
    void create_deveRetornar201() throws Exception {

        when(service.create(chamadoRequest)).thenReturn(chamadoResponse);

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonChamado));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.prioridade").value("BAIXA"));

        verify(service).create(chamadoRequest);
    }

    @Test
    @DisplayName("POST /chamados sem corpo deve retornar 400")
    void create_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /chamados com título e descrição em branco deve retornar 400")
    void create_camposEmBranco_deveRetornar400() throws Exception {

        ChamadoRequest emBranco = new ChamadoRequest("", " ");
        String jsonEmBranco = objectMapper.writeValueAsString(emBranco);

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonEmBranco));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /chamados/{id}/assumir deve retornar 200")
    void assumir_deveRetornar200() throws Exception {

        when(service.assumir(7L)).thenReturn(chamadoResponse);

        ResultActions result = mockMvc.perform(post(url + "/7/assumir"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.responsavelNome").value("Joao"))
                .andExpect(jsonPath("$.podeAtender").value(true));

        verify(service).assumir(7L);
    }

    @Test
    @DisplayName("POST /chamados/{id}/respostas deve retornar 201")
    void responder_deveRetornar201() throws Exception {

        when(service.responder(7L, respostaRequest)).thenReturn(chamadoResponse);

        ResultActions result = mockMvc.perform(post(url + "/7/respostas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonResposta));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.respostas[0].respostaMsg").value("Vou verificar"));

        verify(service).responder(7L, respostaRequest);
    }

    @Test
    @DisplayName("POST /chamados/{id}/respostas sem corpo deve retornar 400")
    void responder_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/7/respostas")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /chamados/{id}/respostas com mensagem em branco deve retornar 400")
    void responder_mensagemEmBranco_deveRetornar400() throws Exception {

        String jsonEmBranco = objectMapper.writeValueAsString(new RespostaChamRequest(" "));

        ResultActions result = mockMvc.perform(post(url + "/7/respostas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonEmBranco));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT /chamados/{id}/finalizar deve retornar 200")
    void finalizar_deveRetornar200() throws Exception {

        when(service.finalizar(7L)).thenReturn(chamadoResponse);

        ResultActions result = mockMvc.perform(put(url + "/7/finalizar"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(service).finalizar(7L);
    }
}