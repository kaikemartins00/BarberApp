package com.kaikeMartins.barberapp_backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kaikeMartins.barberapp_backend.dto.ConversaRequest;
import com.kaikeMartins.barberapp_backend.dto.ConversaResponse;
import com.kaikeMartins.barberapp_backend.dto.MensagemRequest;
import com.kaikeMartins.barberapp_backend.dto.MensagemResponse;
import com.kaikeMartins.barberapp_backend.service.ConversaService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConversaControllerTest {

    @Mock
    private ConversaService service;

    @InjectMocks
    private ConversaController controller;

    private ConversaRequest conversaRequest;

    private ConversaResponse conversaResponse;

    private MensagemRequest mensagemRequest;

    private MensagemResponse mensagemResponse;

    private MockMvc mockMvc;

    private String jsonRequest;

    private String jsonMensagemRequest;

    private String url;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).alwaysDo(print()).build();
        url = "/conversas";
        conversaRequest = new ConversaRequest(1L, null);
        conversaResponse = new ConversaResponse(1L, "kaike", "Oi, tudo bem?", LocalDateTime.now(), 1L, false);
        mensagemRequest = new MensagemRequest("Vou me atrasar um pouco, mas irei");
        mensagemResponse = new MensagemResponse(1L, "Tudo bem, estou no aguardo", LocalDateTime.now(), true, false);
        jsonRequest = objectMapper.writeValueAsString(conversaRequest);
        jsonMensagemRequest = objectMapper.writeValueAsString(mensagemRequest);
    }

    @Test
    @DisplayName("GET /conversas deve retornar 200 e as conversas do usuário")
    void minhasConversas_deveRetornar200() throws Exception {
        when(service.minhasConversas()).thenReturn(List.of(conversaResponse));

        ResultActions result = mockMvc.perform(get(url));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].interlocutorNome").value("kaike"))
                .andExpect(jsonPath("$[0].encerrada").value(false));

        verify(service).minhasConversas();
    }

    @Test
    @DisplayName("POST /conversas deve retornar 201")
    void iniciar_deveRetornar201() throws Exception {

        when(service.iniciar(conversaRequest)).thenReturn(conversaResponse);

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.interlocutorNome").value("kaike"));

        verify(service).iniciar(conversaRequest);
    }

    @Test
    @DisplayName("POST /conversas sem corpo deve retornar 400")
    void iniciar_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /conversas sem ids deve repassar ao service, que valida conforme o perfil")
    void iniciar_semIds_deveRepassarAoService() throws Exception {

        String jsonVazio = "{}";
        when(service.iniciar(any(ConversaRequest.class))).thenReturn(conversaResponse);

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonVazio));

        result.andExpect(status().isCreated());

        verify(service).iniciar(new ConversaRequest(null, null));
    }

    @Test
    @DisplayName("GET /conversas/{id}/mensagens sem 'depois' deve repassar null ao service")
    void mensagens_semDepois_deveRetornar200() throws Exception {

        when(service.mensagens(1L, null)).thenReturn(List.of(mensagemResponse));

        ResultActions result = mockMvc.perform(get(url + "/1/mensagens"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].minha").value(true));

        verify(service).mensagens(1L, null);
    }

    @Test
    @DisplayName("GET /conversas/{id}/mensagens com 'depois' deve repassar o valor ao service")
    void mensagens_comDepois_deveRetornar200() throws Exception {

        when(service.mensagens(1L, 10L)).thenReturn(List.of());

        ResultActions result = mockMvc.perform(get(url + "/1/mensagens").param("depois", "10"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).mensagens(1L, 10L);
    }

    @Test
    @DisplayName("GET /conversas/{id}/mensagens com 'depois' inválido deve retornar 400")
    void mensagens_depoisInvalido_deveRetornar400() throws Exception {

        String depoisInvalido = "abc";

        ResultActions result = mockMvc.perform(get(url + "/1/mensagens").param("depois", depoisInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /conversas/{id}/mensagens deve retornar 201")
    void enviar_deveRetornar201() throws Exception {

        when(service.enviar(1L, mensagemRequest)).thenReturn(mensagemResponse);

        ResultActions result = mockMvc.perform(post(url + "/1/mensagens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMensagemRequest));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Tudo bem, estou no aguardo"))
                .andExpect(jsonPath("$.minha").value(true));

        verify(service).enviar(1L, mensagemRequest);
    }

    @Test
    @DisplayName("POST /conversas/{id}/mensagens sem corpo deve retornar 400")
    void enviar_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/1/mensagens")
                .contentType(MediaType.APPLICATION_JSON));


        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /conversas/{id}/mensagens com texto em branco deve retornar 400")
    void enviar_textoEmBranco_deveRetornar400() throws Exception {

        String jsonEmBranco = objectMapper.writeValueAsString(new MensagemRequest("  "));

        ResultActions result = mockMvc.perform(post(url + "/1/mensagens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonEmBranco));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /conversas/{id}/mensagens com mais de 1000 caracteres deve retornar 400")
    void enviar_textoMuitoLongo_deveRetornar400() throws Exception {

        String jsonLongo = objectMapper.writeValueAsString(new MensagemRequest("a".repeat(1001)));

        ResultActions result = mockMvc.perform(post(url + "/1/mensagens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonLongo));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT /conversas/{id}/lidas deve retornar 204")
    void marcarComoLidas_deveRetornar204() throws Exception {

        ResultActions result = mockMvc.perform(put(url + "/1/lidas"));

        result.andExpect(status().isNoContent());

        verify(service).marcarComoLidas(1L);
    }

    @Test
    @DisplayName("PUT /conversas/{id}/finalizar deve retornar 200")
    void finalizar_deveRetornar200() throws Exception {

        when(service.finalizar(1L)).thenReturn(conversaResponse);

        ResultActions result = mockMvc.perform(put(url + "/1/finalizar"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(service).finalizar(1L);
    }

    @Test
    @DisplayName("PUT /conversas/{id}/reabrir deve retornar 200")
    void reabrir_deveRetornar200() throws Exception {

        when(service.reabrir(1L)).thenReturn(conversaResponse);

        ResultActions result = mockMvc.perform(put(url + "/1/reabrir"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(service).reabrir(1L);
    }
}