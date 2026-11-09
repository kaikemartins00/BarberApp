package com.kaikeMartins.barberapp_backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.dto.ServicoRequest;
import com.kaikeMartins.barberapp_backend.service.ServicoService;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServicoControllerTest {

    @Mock
    private ServicoService service;

    @InjectMocks
    private ServicoController controller;

    private ServicoRequest servicoRequest;

    private ServicoEntity corte;

    private ServicoEntity barba;

    private MockMvc mockMvc;

    private String jsonRequest;

    private String url;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).alwaysDo(print()).build();
        url = "/servicos";
        servicoRequest = new ServicoRequest("Corte", 40.0);
        corte = ServicoEntity.builder().id(2L).nome("Corte").preco(40.0).build();
        barba = ServicoEntity.builder().id(3L).nome("Barba").preco(30.0).build();
        jsonRequest = objectMapper.writeValueAsString(servicoRequest);
    }

    @Test
    @DisplayName("GET /servicos deve retornar 200 e a lista de serviços")
    void findAll_deveRetornar200() throws Exception {

        when(service.findAll()).thenReturn(List.of(corte, barba));

        ResultActions result = mockMvc.perform(get(url));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Corte"))
                .andExpect(jsonPath("$[1].preco").value(30.0));

        verify(service).findAll();
    }

    @Test
    @DisplayName("GET /servicos/{id} deve retornar 200 e buscar pelo id")
    void findById_deveRetornar200() throws Exception {

        when(service.findById(2L)).thenReturn(corte);

        ResultActions result = mockMvc.perform(get(url + "/2"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nome").value("Corte"));

        verify(service).findById(2L);
    }

    @Test
    @DisplayName("GET /servicos/{id} com id não numérico deve retornar 400")
    void findById_idInvalido_deveRetornar400() throws Exception {

        String idInvalido = "abc";

        ResultActions result = mockMvc.perform(get(url + "/" + idInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /servicos deve retornar 201")
    void create_deveRetornar201() throws Exception {

        when(service.create(servicoRequest)).thenReturn(corte);

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Corte"))
                .andExpect(jsonPath("$.preco").value(40.0));

        verify(service).create(servicoRequest);
    }

    @Test
    @DisplayName("POST /servicos sem corpo deve retornar 400")
    void create_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /servicos com nome em branco deve retornar 400")
    void create_nomeEmBranco_deveRetornar400() throws Exception {

        String jsonInvalido = objectMapper.writeValueAsString(new ServicoRequest("", 40.0));

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /servicos com preço zero ou negativo deve retornar 400")
    void create_precoNaoPositivo_deveRetornar400() throws Exception {

        String jsonInvalido = objectMapper.writeValueAsString(new ServicoRequest("Corte", 0.0));

        ResultActions result = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT /servicos/{id} deve retornar 200 e delegar ao service")
    void update_deveRetornar200() throws Exception {
        // Given
        ServicoRequest atualizacao = new ServicoRequest("Corte Premium", 60.0);
        ServicoEntity atualizado = ServicoEntity.builder().id(2L).nome("Corte Premium").preco(60.0).build();
        when(service.update(2L, atualizacao)).thenReturn(atualizado);

        ResultActions result = mockMvc.perform(put(url + "/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(atualizacao)));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Corte Premium"))
                .andExpect(jsonPath("$.preco").value(60.0));

        verify(service).update(2L, atualizacao);
    }

    @Test
    @DisplayName("PUT /servicos/{id} com corpo inválido deve retornar 400")
    void update_corpoInvalido_deveRetornar400() throws Exception {

        String jsonInvalido = objectMapper.writeValueAsString(new ServicoRequest(" ", -5.0));

        ResultActions result = mockMvc.perform(put(url + "/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("DELETE /servicos/{id} deve retornar 204")
    void delete_deveRetornar204() throws Exception {

        ResultActions result = mockMvc.perform(delete(url + "/2"));

        result.andExpect(status().isNoContent());

        verify(service).delete(2L);
    }
}