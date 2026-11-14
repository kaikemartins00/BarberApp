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
import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.service.AtendimentoServicoService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AtendimentoServicoControllerTest {

    @Mock
    private AtendimentoServicoService service;

    @InjectMocks
    private AtendimentoServicoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ContaAtendimentoResponse conta() {
        return new ContaAtendimentoResponse(10L,
                List.of(new ServicoAtendimentoResponse(1L, 3L, "Corte", new BigDecimal("40.00"), false)),
                new BigDecimal("40.00"), "PENDENTE");
    }

    @Test
    @DisplayName("GET /agendamentos/{id}/servicos deve retornar a conta")
    void consultar() throws Exception {
        when(service.consultar(10L)).thenReturn(conta());

        mockMvc.perform(get("/agendamentos/10/servicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentoId").value(10))
                .andExpect(jsonPath("$.servicos[0].servicoNome").value("Corte"))
                .andExpect(jsonPath("$.valorTotal").value(40.00));
    }

    @Test
    @DisplayName("POST deve adicionar o serviço e retornar a conta")
    void adicionar() throws Exception {
        when(service.adicionar(eq(10L), any(AdicionarServicoAtendimentoRequest.class))).thenReturn(conta());

        mockMvc.perform(post("/agendamentos/10/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicoId\":3,\"valorCobrado\":15.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagamentoStatus").value("PENDENTE"));
    }

    @Test
    @DisplayName("POST deve retornar 400 quando falta o serviço")
    void adicionarSemServico() throws Exception {
        mockMvc.perform(post("/agendamentos/10/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorCobrado\":15.00}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST deve retornar 400 quando o valor é menor que o mínimo")
    void adicionarValorInvalido() throws Exception {
        mockMvc.perform(post("/agendamentos/10/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicoId\":3,\"valorCobrado\":0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PATCH deve alterar o valor do item")
    void alterarValor() throws Exception {
        when(service.alterarValor(eq(10L), eq(1L), any(AlterarValorServicoRequest.class))).thenReturn(conta());

        mockMvc.perform(patch("/agendamentos/10/servicos/1/valor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorCobrado\":35.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentoId").value(10));
    }

    @Test
    @DisplayName("PATCH deve retornar 400 quando o valor não é informado")
    void alterarValorAusente() throws Exception {
        mockMvc.perform(patch("/agendamentos/10/servicos/1/valor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("DELETE deve remover o serviço extra")
    void remover() throws Exception {
        when(service.removerExtra(10L, 1L)).thenReturn(conta());

        mockMvc.perform(delete("/agendamentos/10/servicos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentoId").value(10));
    }

    @Test
    @DisplayName("DELETE deve retornar 400 quando o serviço não pode ser removido")
    void removerRegraDeNegocio() throws Exception {
        when(service.removerExtra(10L, 1L)).thenThrow(new BadRequestException("Não é possível remover."));

        mockMvc.perform(delete("/agendamentos/10/servicos/1"))
                .andExpect(status().isBadRequest());
    }
}
