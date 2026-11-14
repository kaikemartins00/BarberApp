package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.AvaliacaoRequest;
import com.kaikeMartins.barberapp_backend.dto.AvaliacaoResponse;
import com.kaikeMartins.barberapp_backend.dto.AvaliacaoResumoResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.GlobalExceptionHandler;
import com.kaikeMartins.barberapp_backend.service.AvaliacaoService;
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
class AvaliacaoControllerTest {

    @Mock
    private AvaliacaoService service;

    @InjectMocks
    private AvaliacaoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /avaliacoes deve retornar 201 e a avaliação criada")
    void criarRetorna201() throws Exception {
        when(service.criar(new AvaliacaoRequest(10L, 5, "Ótimo", List.of("pontual"))))
                .thenReturn(new AvaliacaoResponse(1L, 10L, 2L, 5, "Ótimo", List.of("pontual"), LocalDateTime.now()));

        mockMvc.perform(post("/avaliacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamentoId\":10,\"nota\":5,\"comentario\":\"Ótimo\",\"caracteristicas\":[\"pontual\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nota").value(5))
                .andExpect(jsonPath("$.tags[0]").value("pontual"));
    }

    @Test
    @DisplayName("POST /avaliacoes deve retornar 400 quando a nota está fora do intervalo")
    void criarNotaInvalida() throws Exception {
        mockMvc.perform(post("/avaliacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamentoId\":10,\"nota\":7}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).criar(any());
    }

    @Test
    @DisplayName("POST /avaliacoes deve retornar 400 quando o serviço rejeita a avaliação")
    void criarRejeitada() throws Exception {
        when(service.criar(any(AvaliacaoRequest.class)))
                .thenThrow(new BadRequestException("Esse atendimento já foi avaliado."));

        mockMvc.perform(post("/avaliacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamentoId\":10,\"nota\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Esse atendimento já foi avaliado."));
    }

    @Test
    @DisplayName("GET /avaliacoes/agendamentos/{id} deve informar se já foi avaliado")
    void porAgendamento() throws Exception {
        when(service.porAgendamento(10L)).thenReturn(true);

        mockMvc.perform(get("/avaliacoes/agendamentos/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avaliado").value(true));
    }

    @Test
    @DisplayName("GET /avaliacoes/barbeiros/{id}/resumo deve retornar média e quantidade")
    void resumo() throws Exception {
        when(service.resumo(2L)).thenReturn(new AvaliacaoResumoResponse(4.5, 2));

        mockMvc.perform(get("/avaliacoes/barbeiros/2/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media").value(4.5))
                .andExpect(jsonPath("$.quantidade").value(2));
    }
}
