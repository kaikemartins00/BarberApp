package com.kaikeMartins.barberapp_backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroResponse;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroUpdateRequest;
import com.kaikeMartins.barberapp_backend.dto.CancelamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.CancelarDiaRequest;
import com.kaikeMartins.barberapp_backend.dto.HorariosRequest;
import com.kaikeMartins.barberapp_backend.dto.ServicosIdsRequest;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BarbeiroControllerTest {

    @Mock
    private BarbeiroService service;

    @InjectMocks
    private BarbeiroController controller;

    private BarbeiroUpdateRequest updateRequest;

    private ServicosIdsRequest servicosRequest;

    private HorariosRequest horariosRequest;

    private CancelarDiaRequest cancelarDiaRequest;

    private BarbeiroResponse barbeiroResponse;

    private CancelamentoResponse cancelamentoResponse;

    private MockMvc mockMvc;

    private String jsonUpdate;

    private String jsonServicos;

    private String jsonHorarios;

    private String jsonCancelarDia;

    private String url;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).alwaysDo(print()).build();
        url = "/barbeiros";
        LocalDate amanha = LocalDate.now().plusDays(1);

        updateRequest = new BarbeiroUpdateRequest("joao@email.com", "1novaSenha", "http://foto.com/j.jpg");
        servicosRequest = new ServicosIdsRequest(List.of(1L, 2L));
        horariosRequest = new HorariosRequest(List.of(
                LocalDateTime.of(amanha, LocalTime.of(9, 0)),
                LocalDateTime.of(amanha, LocalTime.of(10, 0))));
        cancelarDiaRequest = new CancelarDiaRequest(amanha, null, "Doente");

        barbeiroResponse = new BarbeiroResponse(3L, "Joao", "http://foto.com/j.jpg", Set.of(),
                List.of(LocalDateTime.of(amanha, LocalTime.of(9, 0))));
        cancelamentoResponse = new CancelamentoResponse(3, 5);

        jsonUpdate = objectMapper.writeValueAsString(updateRequest);
        jsonServicos = objectMapper.writeValueAsString(servicosRequest);
        jsonHorarios = objectMapper.writeValueAsString(horariosRequest);
        jsonCancelarDia = objectMapper.writeValueAsString(cancelarDiaRequest);
    }

    @Test
    @DisplayName("GET /barbeiros deve retornar 200 e a lista")
    void findAll_deveRetornar200() throws Exception {

        when(service.findAll()).thenReturn(List.of(barbeiroResponse));

        ResultActions result = mockMvc.perform(get(url));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Joao"));

        verify(service).findAll();
    }

    @Test
    @DisplayName("GET /barbeiros/{id} deve retornar 200 e buscar pelo id")
    void findById_deveRetornar200() throws Exception {

        when(service.findById(3L)).thenReturn(barbeiroResponse);

        ResultActions result = mockMvc.perform(get(url + "/3"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.fotoUrl").value("http://foto.com/j.jpg"));

        verify(service).findById(3L);
    }

    @Test
    @DisplayName("GET /barbeiros/{id} com id não numérico deve retornar 400")
    void findById_idInvalido_deveRetornar400() throws Exception {

        String idInvalido = "abc";

        ResultActions result = mockMvc.perform(get(url + "/" + idInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT /barbeiros/{id} deve retornar 200 e delegar ao service")
    void update_deveRetornar200() throws Exception {

        when(service.update(3L, updateRequest)).thenReturn(barbeiroResponse);

        ResultActions result = mockMvc.perform(put(url + "/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonUpdate));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));

        verify(service).update(3L, updateRequest);
    }

    @Test
    @DisplayName("PUT /barbeiros/{id} sem corpo deve retornar 400")
    void update_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(put(url + "/3")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT /barbeiros/{id} com e-mail inválido deve retornar 400")
    void update_emailInvalido_deveRetornar400() throws Exception {

        BarbeiroUpdateRequest invalido = new BarbeiroUpdateRequest("emailinvalido", null, null);
        String jsonInvalido = objectMapper.writeValueAsString(invalido);

        ResultActions result = mockMvc.perform(put(url + "/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT /barbeiros/{id}/servicos deve retornar 200")
    void atualizarServicos_deveRetornar200() throws Exception {

        when(service.atualizarServicos(3L, servicosRequest)).thenReturn(barbeiroResponse);

        ResultActions result = mockMvc.perform(put(url + "/3/servicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonServicos));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));

        verify(service).atualizarServicos(3L, servicosRequest);
    }

    @Test
    @DisplayName("PUT /barbeiros/{id}/servicos sem a lista de ids deve retornar 400")
    void atualizarServicos_semLista_deveRetornar400() throws Exception {

        String jsonVazio = "{}";

        ResultActions result = mockMvc.perform(put(url + "/3/servicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonVazio));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PUT /barbeiros/{id}/horarios deve retornar 200")
    void atualizarHorarios_deveRetornar200() throws Exception {

        when(service.atualizarHorarios(3L, horariosRequest)).thenReturn(barbeiroResponse);

        ResultActions result = mockMvc.perform(put(url + "/3/horarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonHorarios));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));

        verify(service).atualizarHorarios(3L, horariosRequest);
    }

    @Test
    @DisplayName("PUT /barbeiros/{id}/horarios sem a lista de horários deve retornar 400")
    void atualizarHorarios_semLista_deveRetornar400() throws Exception {

        String jsonVazio = "{}";

        ResultActions result = mockMvc.perform(put(url + "/3/horarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonVazio));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("DELETE /barbeiros/{id} deve retornar 204")
    void delete_deveRetornar204() throws Exception {

        ResultActions result = mockMvc.perform(delete(url + "/3"));

        result.andExpect(status().isNoContent());

        verify(service).delete(3L);
    }

    @Test
    @DisplayName("POST /barbeiros/{id}/cancelar-dia deve retornar 200 e os contadores")
    void cancelarDia_deveRetornar200() throws Exception {

        when(service.cancelarDia(3L, cancelarDiaRequest)).thenReturn(cancelamentoResponse);

        ResultActions result = mockMvc.perform(post(url + "/3/cancelar-dia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonCancelarDia));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentosCancelados").value(3))
                .andExpect(jsonPath("$.horariosRemovidos").value(5));

        verify(service).cancelarDia(3L, cancelarDiaRequest);
    }

    @Test
    @DisplayName("POST /barbeiros/{id}/cancelar-dia com data no passado deve retornar 400")
    void cancelarDia_dataPassada_deveRetornar400() throws Exception {

        CancelarDiaRequest passada = new CancelarDiaRequest(LocalDate.now().minusDays(1), null, null);
        String jsonPassada = objectMapper.writeValueAsString(passada);

        ResultActions result = mockMvc.perform(post(url + "/3/cancelar-dia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPassada));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}