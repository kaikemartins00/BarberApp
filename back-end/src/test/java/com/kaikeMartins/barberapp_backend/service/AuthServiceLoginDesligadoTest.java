package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.dto.LoginRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.RoleRepository;
import com.kaikeMartins.barberapp_backend.repository.UserRepository;
import com.kaikeMartins.barberapp_backend.security.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.barbeiro;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginDesligadoTest {

    @InjectMocks
    private AuthService service;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private UserRepository usuarioRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("Deve impedir o login de barbeiro desligado sem tentar autenticar")
    void loginBarbeiroDesligado() {
        BarbeiroEntity desligado = barbeiro(2L);
        desligado.setDesligadoEm(LocalDateTime.now().minusDays(1));
        when(usuarioRepository.findByEmail("usuario2@email.com")).thenReturn(Optional.of(desligado.getUsuario()));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(desligado));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.login(new LoginRequest("usuario2@email.com", "123456")));

        assertEquals("Este acesso foi desativado. Procure o administrador.", e.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }
}
