package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.FilaEsperaRequest;
import com.kaikeMartins.barberapp_backend.dto.FilaEsperaResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.GlobalExceptionHandler;
import com.kaikeMartins.barberapp_backend.service.FilaEsperaService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FilaEsperaControllerTest {

    @Mock
    private FilaEsperaService service;

    @InjectMocks
    private FilaEsperaController controller;

    private MockMvc mockMvc;

    private LocalDate dia;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        dia = LocalDate.now().plusDays(2);
    }

    private FilaEsperaResponse resposta(String status) {
        return new FilaEsperaResponse(50L, 20L, dia, Set.of(LocalTime.of(10, 0)),
                dia.plusDays(3), LocalTime.of(14, 0), status);
    }

    @Test
    @DisplayName("POST /fila-espera deve retornar 201 e a inscrição")
    void entrarRetorna201() throws Exception {
        when(service.entrar(any(FilaEsperaRequest.class))).thenReturn(resposta("AGUARDANDO"));

        mockMvc.perform(post("/fila-espera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamentoAlternativoId\":20,\"dataPreferida\":\"" + dia + "\",\"horariosAceitos\":[\"10:00:00\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO"));
    }

    @Test
    @DisplayName("POST /fila-espera deve retornar 400 quando não há horários aceitos")
    void entrarSemHorarios() throws Exception {
        mockMvc.perform(post("/fila-espera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamentoAlternativoId\":20,\"dataPreferida\":\"" + dia + "\",\"horariosAceitos\":[]}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).entrar(any());
    }

    @Test
    @DisplayName("POST /fila-espera deve retornar 400 quando o serviço rejeita a entrada")
    void entrarRejeitada() throws Exception {
        when(service.entrar(any(FilaEsperaRequest.class)))
                .thenThrow(new BadRequestException("Você já está na fila desse agendamento."));

        mockMvc.perform(post("/fila-espera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamentoAlternativoId\":20,\"dataPreferida\":\"" + dia + "\",\"horariosAceitos\":[\"10:00:00\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Você já está na fila desse agendamento."));
    }

    @Test
    @DisplayName("GET /fila-espera/{id} deve retornar 200 e a inscrição")
    void consultarRetorna200() throws Exception {
        when(service.consultar(50L)).thenReturn(resposta("AGUARDANDO"));

        mockMvc.perform(get("/fila-espera/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    @DisplayName("DELETE /fila-espera/{id} deve retornar 200 com a inscrição cancelada")
    void sairRetorna200() throws Exception {
        when(service.sair(50L)).thenReturn(resposta("CANCELADO"));

        mockMvc.perform(delete("/fila-espera/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }
}
