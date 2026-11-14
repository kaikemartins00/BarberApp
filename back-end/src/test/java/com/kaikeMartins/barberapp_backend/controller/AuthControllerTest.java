package com.kaikeMartins.barberapp_backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroRequest;
import com.kaikeMartins.barberapp_backend.dto.CadastroRequest;
import com.kaikeMartins.barberapp_backend.dto.LoginRequest;
import com.kaikeMartins.barberapp_backend.dto.TokenResponse;
import com.kaikeMartins.barberapp_backend.dto.ValidarCodigoRequest;
import com.kaikeMartins.barberapp_backend.service.AuthService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService service;

    @InjectMocks
    private AuthController controller;

    private BarbeiroRequest barbeiroRequest;

    private CadastroRequest cadastroRequest;

    private LoginRequest loginRequest;

    private ValidarCodigoRequest validarCodigoRequest;

    private TokenResponse tokenResponse;

    private MockMvc mockMvc;

    private String jsonBarbeiro;

    private String jsonCadastro;

    private String jsonLogin;

    private String jsonCodigo;

    private String url;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).alwaysDo(print()).build();
        url = "/auth";
        barbeiroRequest = new BarbeiroRequest("Joao", "joao@email.com", "Ee123456", "79988888888", null);
        cadastroRequest = new CadastroRequest("Kaike", "kaike@email.com", "Ee123456", true, "1.0");
        loginRequest = new LoginRequest("kaike@email.com", "Ee123456");
        validarCodigoRequest = new ValidarCodigoRequest("kaike@email.com", "123456");
        tokenResponse = new TokenResponse("jwt-token", 3600000L, 1L, List.of("ROLE_CLIENTE"));
        jsonBarbeiro = objectMapper.writeValueAsString(barbeiroRequest);
        jsonCadastro = objectMapper.writeValueAsString(cadastroRequest);
        jsonLogin = objectMapper.writeValueAsString(loginRequest);
        jsonCodigo = objectMapper.writeValueAsString(validarCodigoRequest);
    }

    @Test
    @DisplayName("POST /auth/cadastro-barbeiro deve retornar 201")
    void registerBarbeiro_deveRetornar201() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/cadastro-barbeiro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBarbeiro));

        result.andExpect(status().isCreated());

        verify(service).registerBarbeiro(barbeiroRequest);
    }

    @Test
    @DisplayName("POST /auth/cadastro-barbeiro sem corpo deve retornar 400")
    void registerBarbeiro_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/cadastro-barbeiro")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /auth/cadastro-barbeiro com campos inválidos deve retornar 400")
    void registerBarbeiro_camposInvalidos_deveRetornar400() throws Exception {

        BarbeiroRequest invalido = new BarbeiroRequest("", "emailinvalido", "", "", null);
        String jsonInvalido = objectMapper.writeValueAsString(invalido);

        ResultActions result = mockMvc.perform(post(url + "/cadastro-barbeiro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /auth/cadastro deve retornar 201")
    void register_deveRetornar201() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonCadastro));

        result.andExpect(status().isCreated());

        verify(service).register(cadastroRequest);
    }

    @Test
    @DisplayName("POST /auth/cadastro sem corpo deve retornar 400")
    void register_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/cadastro")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /auth/cadastro com campos inválidos deve retornar 400")
    void register_camposInvalidos_deveRetornar400() throws Exception {

        CadastroRequest invalido = new CadastroRequest("", "emailinvalido", "", false, null);
        String jsonInvalido = objectMapper.writeValueAsString(invalido);

        ResultActions result = mockMvc.perform(post(url + "/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /auth/login deve retornar 200 e o token")
    void login_deveRetornar200() throws Exception {

        when(service.login(loginRequest)).thenReturn(tokenResponse);

        ResultActions result = mockMvc.perform(post(url + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonLogin));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.expirationTime").value(3600000))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CLIENTE"));

        verify(service).login(loginRequest);
    }

    @Test
    @DisplayName("POST /auth/login sem corpo deve retornar 400")
    void login_semCorpo_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/login")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /auth/validar-codigo deve retornar 200")
    void validarCodigo_deveRetornar200() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/validar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonCodigo));

        result.andExpect(status().isOk());

        verify(service).validarCodigo(validarCodigoRequest);
    }

    @Test
    @DisplayName("POST /auth/validar-codigo com campos inválidos deve retornar 400")
    void validarCodigo_camposInvalidos_deveRetornar400() throws Exception {

        ValidarCodigoRequest invalido = new ValidarCodigoRequest("emailinvalido", "");
        String jsonInvalido = objectMapper.writeValueAsString(invalido);

        ResultActions result = mockMvc.perform(post(url + "/validar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonInvalido));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("POST /auth/reenviar-codigo deve retornar 200 e repassar o e-mail")
    void reenviarCodigo_deveRetornar200() throws Exception {

        String email = "kaike@email.com";

        ResultActions result = mockMvc.perform(post(url + "/reenviar-codigo")
                .param("email", email));

        result.andExpect(status().isOk());

        verify(service).reenviarCodigo(email);
    }

    @Test
    @DisplayName("POST /auth/reenviar-codigo sem e-mail deve retornar 400")
    void reenviarCodigo_semEmail_deveRetornar400() throws Exception {

        ResultActions result = mockMvc.perform(post(url + "/reenviar-codigo"));

        result.andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}