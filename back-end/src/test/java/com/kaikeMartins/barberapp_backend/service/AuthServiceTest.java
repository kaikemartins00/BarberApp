package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.RoleEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.RoleRepository;
import com.kaikeMartins.barberapp_backend.repository.UserRepository;
import com.kaikeMartins.barberapp_backend.security.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    private static final String MSG_CODIGO_INVALIDO = "Código inválido ou expirado. Solicite um novo código.";
    private static final String MSG_LIMITE = "Você atingiu o limite de 3 e-mails por dia. Tente novamente amanhã.";

    @InjectMocks
    private AuthService authService;

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

    private RoleEntity roleCliente;

    private RoleEntity roleBarbeiro;

    private UsuarioEntity usuarioVerificado;

    private UsuarioEntity usuarioNaoVerificado;

    private UsuarioEntity usuarioRedefinicao;

    private BarbeiroRequest barbeiroRequest;

    private CadastroRequest cadastroRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationTime", 3600000L);

        roleCliente = RoleEntity.builder().name("ROLE_CLIENTE").build();
        roleBarbeiro = RoleEntity.builder().name("ROLE_BARBEIRO").build();

        usuarioVerificado = UsuarioEntity.builder()
                .id(1L)
                .email("kaike@email.com")
                .senha("hash")
                .verificado(true)
                .roles(new HashSet<>(Set.of(roleCliente)))
                .build();

        usuarioNaoVerificado = UsuarioEntity.builder()
                .id(1L)
                .email("kaike@email.com")
                .senha("hash")
                .verificado(false)
                .codigoVerificacao("123456")
                .codigoExpiracao(LocalDateTime.now().plusMinutes(5))
                .roles(new HashSet<>(Set.of(roleCliente)))
                .build();

        usuarioRedefinicao = UsuarioEntity.builder()
                .id(1L)
                .email("kaike@email.com")
                .senha("hash")
                .verificado(true)
                .roles(new HashSet<>(Set.of(roleCliente)))
                .build();
        usuarioRedefinicao.setCodigoRedefinicao("hash-codigo");
        usuarioRedefinicao.setRedefinicaoExpiraEm(LocalDateTime.now().plusMinutes(5));
        usuarioRedefinicao.setTentativasRedefinicao(0);

        barbeiroRequest = new BarbeiroRequest("Joao", "joao@email.com", "123456", "79988888888", "http://foto.com/j.jpg");
        cadastroRequest = new CadastroRequest("Kaike", "kaike@email.com", "123456", true, "1.0");
    }

    private LocalDate hoje() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o e-mail do barbeiro já existir")
    void registerBarbeiroEmailExistsError() {
        when(usuarioRepository.existsByEmail("joao@email.com")).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.registerBarbeiro(barbeiroRequest));

        assertThat(e.getMessage(), is("O email já existe."));
        verify(usuarioRepository, never()).save(any());
        verify(barbeiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve registrar o barbeiro como verificado, utilizando a role existente")
    void registerBarbeiroSuccess() {
        when(usuarioRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_BARBEIRO")).thenReturn(Optional.of(roleBarbeiro));
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(i -> i.getArgument(0));

        authService.registerBarbeiro(barbeiroRequest);

        ArgumentCaptor<UsuarioEntity> usuarioCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
        ArgumentCaptor<BarbeiroEntity> barbeiroCaptor = ArgumentCaptor.forClass(BarbeiroEntity.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(barbeiroRepository).save(barbeiroCaptor.capture());
        verify(roleRepository, never()).save(any());

        assertEquals("joao@email.com", usuarioCaptor.getValue().getEmail());
        assertEquals("hash", usuarioCaptor.getValue().getSenha());
        assertTrue(usuarioCaptor.getValue().isVerificado());
        assertTrue(usuarioCaptor.getValue().getRoles().contains(roleBarbeiro));
        assertEquals("Joao", barbeiroCaptor.getValue().getNome());
        assertEquals("79988888888", barbeiroCaptor.getValue().getTelefone());
        assertEquals("http://foto.com/j.jpg", barbeiroCaptor.getValue().getFotoUrl());
        assertSame(usuarioCaptor.getValue(), barbeiroCaptor.getValue().getUsuario());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Deve criar a role de barbeiro quando ela não existir")
    void registerBarbeiroCreatesRoleSuccess() {
        when(usuarioRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_BARBEIRO")).thenReturn(Optional.empty());
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(i -> i.getArgument(0));

        authService.registerBarbeiro(barbeiroRequest);

        ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
        verify(roleRepository).save(captor.capture());

        assertEquals("ROLE_BARBEIRO", captor.getValue().getName());
        verify(barbeiroRepository).save(any(BarbeiroEntity.class));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao realizar cadastro com um e-mail verificado")
    void registerVerifiedEmailError() {
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioVerificado));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.register(cadastroRequest));

        assertThat(e.getMessage(), is("O email já existe."));
        verify(clienteRepository, never()).save(any());
        verify(emailService, never()).enviarCodigo(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve reenviar o código ao realizar o cadastro com um e-mail não verificado")
    void registerUnverifiedEmailResendsCode() {
        usuarioNaoVerificado.setCodigoVerificacao("111111");
        usuarioNaoVerificado.setCodigoExpiracao(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioNaoVerificado));

        authService.register(cadastroRequest);

        assertNotEquals("111111", usuarioNaoVerificado.getCodigoVerificacao());
        assertTrue(usuarioNaoVerificado.getCodigoExpiracao().isAfter(LocalDateTime.now()));
        verify(emailService).enviarCodigo("kaike@email.com", usuarioNaoVerificado.getCodigoVerificacao());
        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, roleRepository);
    }

    @Test
    @DisplayName("Deve registrar o novo cliente como não verificado e enviar o código")
    void registerSuccess() {
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_CLIENTE")).thenReturn(Optional.of(roleCliente));
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(cadastroRequest);

        ArgumentCaptor<UsuarioEntity> usuarioCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
        ArgumentCaptor<ClienteEntity> clienteCaptor = ArgumentCaptor.forClass(ClienteEntity.class);
        verify(usuarioRepository, times(2)).save(usuarioCaptor.capture());
        verify(clienteRepository).save(clienteCaptor.capture());
        UsuarioEntity usuario = usuarioCaptor.getValue();

        assertEquals("kaike@email.com", usuario.getEmail());
        assertEquals("hash", usuario.getSenha());
        assertFalse(usuario.isVerificado());
        assertTrue(usuario.getRoles().contains(roleCliente));
        assertTrue(usuario.getCodigoVerificacao().matches("\\d{6}"));
        assertTrue(usuario.getCodigoExpiracao().isAfter(LocalDateTime.now()));
        assertEquals(1, usuario.getEmailsEnviadosHoje());
        assertEquals(hoje(), usuario.getDataContagemEmails());
        assertEquals("Kaike", clienteCaptor.getValue().getNome());
        assertEquals("", clienteCaptor.getValue().getTelefone());
        assertEquals("1.0", clienteCaptor.getValue().getVersaoTermos());
        assertNotNull(clienteCaptor.getValue().getAceitouTermosEm());
        assertSame(usuario, clienteCaptor.getValue().getUsuario());
        verify(emailService).enviarCodigo("kaike@email.com", usuario.getCodigoVerificacao());
        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar a role de cliente quando ela não existir")
    void registerCreatesRoleSuccess() {
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_CLIENTE")).thenReturn(Optional.empty());
        when(roleRepository.save(any(RoleEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(cadastroRequest);

        ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
        verify(roleRepository).save(captor.capture());

        assertEquals("ROLE_CLIENTE", captor.getValue().getName());
        verify(clienteRepository).save(any(ClienteEntity.class));
        verify(emailService).enviarCodigo(eq("kaike@email.com"), anyString());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao validar o código de um usuário inexistente")
    void validarCodigoNotFoundError() {
        when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> authService.validarCodigo(new ValidarCodigoRequest("x@email.com", "123456")));

        assertThat(e.getMessage(), is("Cliente não encontrado"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao validar o código de uma conta já verificada")
    void validarCodigoAlreadyVerifiedError() {
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioVerificado));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.validarCodigo(new ValidarCodigoRequest("kaike@email.com", "123456")));

        assertThat(e.getMessage(), is("Conta já verificada."));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o código estiver expirado")
    void validarCodigoExpiredError() {
        usuarioNaoVerificado.setCodigoExpiracao(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioNaoVerificado));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.validarCodigo(new ValidarCodigoRequest("kaike@email.com", "123456")));

        assertThat(e.getMessage(), is("Código expirado. Solicite um novo."));
        assertFalse(usuarioNaoVerificado.isVerificado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o código estiver incorreto")
    void validarCodigoInvalidError() {
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioNaoVerificado));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.validarCodigo(new ValidarCodigoRequest("kaike@email.com", "000000")));

        assertThat(e.getMessage(), is("Código inválido."));
        assertFalse(usuarioNaoVerificado.isVerificado());
        assertEquals("123456", usuarioNaoVerificado.getCodigoVerificacao());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve verificar a conta e limpar o código")
    void validarCodigoSuccess() {
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioNaoVerificado));

        authService.validarCodigo(new ValidarCodigoRequest("kaike@email.com", "123456"));

        assertTrue(usuarioNaoVerificado.isVerificado());
        assertNull(usuarioNaoVerificado.getCodigoVerificacao());
        assertNull(usuarioNaoVerificado.getCodigoExpiracao());
        verify(usuarioRepository).save(usuarioNaoVerificado);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao reenviar código para um usuário inexistente")
    void reenviarCodigoNotFoundError() {
        when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> authService.reenviarCodigo("x@email.com"));

        assertThat(e.getMessage(), is("Cliente não encontrado"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao reenviar código para uma conta verificada")
    void reenviarCodigoAlreadyVerifiedError() {
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioVerificado));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.reenviarCodigo("kaike@email.com"));

        assertThat(e.getMessage(), is("Conta já verificada."));
        verify(emailService, never()).enviarCodigo(anyString(), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve gerar um novo código, contar o e-mail e enviá-lo")
    void reenviarCodigoSuccess() {
        usuarioNaoVerificado.setCodigoVerificacao("111111");
        usuarioNaoVerificado.setCodigoExpiracao(LocalDateTime.now().minusMinutes(10));
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioNaoVerificado));

        authService.reenviarCodigo("kaike@email.com");

        assertTrue(usuarioNaoVerificado.getCodigoVerificacao().matches("\\d{6}"));
        assertTrue(usuarioNaoVerificado.getCodigoExpiracao().isAfter(LocalDateTime.now()));
        assertEquals(1, usuarioNaoVerificado.getEmailsEnviadosHoje());
        assertEquals(hoje(), usuarioNaoVerificado.getDataContagemEmails());
        verify(usuarioRepository, times(2)).save(usuarioNaoVerificado);
        verify(emailService).enviarCodigo("kaike@email.com", usuarioNaoVerificado.getCodigoVerificacao());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao atingir o limite diário de e-mails")
    void reenviarCodigoDailyLimitError() {
        usuarioNaoVerificado.setEmailsEnviadosHoje(3);
        usuarioNaoVerificado.setDataContagemEmails(hoje());
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioNaoVerificado));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.reenviarCodigo("kaike@email.com"));

        assertThat(e.getMessage(), is(MSG_LIMITE));
        assertEquals("123456", usuarioNaoVerificado.getCodigoVerificacao());
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Deve zerar o contador quando a data da contagem for de outro dia")
    void reenviarCodigoResetsCounterOnNewDaySuccess() {
        usuarioNaoVerificado.setEmailsEnviadosHoje(3);
        usuarioNaoVerificado.setDataContagemEmails(hoje().minusDays(1));
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioNaoVerificado));

        authService.reenviarCodigo("kaike@email.com");

        assertEquals(1, usuarioNaoVerificado.getEmailsEnviadosHoje());
        assertEquals(hoje(), usuarioNaoVerificado.getDataContagemEmails());
        verify(emailService).enviarCodigo(eq("kaike@email.com"), anyString());
    }

    @Test
    @DisplayName("Não deve fazer nada ao solicitar redefinição para um e-mail inexistente")
    void solicitarRedefinicaoNotFoundSuccess() {
        when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        authService.solicitarRedefinicao("x@email.com");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService, passwordEncoder);
    }

    @Test
    @DisplayName("Deve gerar o código de redefinição, guardá-lo codificado e enviá-lo por e-mail")
    void solicitarRedefinicaoSuccess() {
        usuarioRedefinicao.setCodigoRedefinicao(null);
        usuarioRedefinicao.setRedefinicaoExpiraEm(null);
        usuarioRedefinicao.setTentativasRedefinicao(4);
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioRedefinicao));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-novo");

        authService.solicitarRedefinicao("kaike@email.com");

        ArgumentCaptor<String> codigoCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigoRedefinicao(eq("kaike@email.com"), codigoCaptor.capture());
        verify(passwordEncoder).encode(codigoCaptor.getValue());

        assertTrue(codigoCaptor.getValue().matches("\\d{6}"));
        assertEquals("hash-novo", usuarioRedefinicao.getCodigoRedefinicao());
        assertTrue(usuarioRedefinicao.getRedefinicaoExpiraEm().isAfter(LocalDateTime.now().plusMinutes(9)));
        assertEquals(0, usuarioRedefinicao.getTentativasRedefinicao());
        assertEquals(1, usuarioRedefinicao.getEmailsEnviadosHoje());
        verify(usuarioRepository, times(2)).save(usuarioRedefinicao);
    }

    @Test
    @DisplayName("Não deve gerar novo código quando o pedido anterior tem menos de 60 segundos")
    void solicitarRedefinicaoTooSoonSuccess() {
        usuarioRedefinicao.setRedefinicaoExpiraEm(LocalDateTime.now().plusMinutes(10).minusSeconds(30));
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioRedefinicao));

        authService.solicitarRedefinicao("kaike@email.com");

        assertEquals("hash-codigo", usuarioRedefinicao.getCodigoRedefinicao());
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService, passwordEncoder);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao atingir o limite diário na solicitação de redefinição")
    void solicitarRedefinicaoDailyLimitError() {
        usuarioRedefinicao.setRedefinicaoExpiraEm(null);
        usuarioRedefinicao.setEmailsEnviadosHoje(3);
        usuarioRedefinicao.setDataContagemEmails(hoje());
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioRedefinicao));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.solicitarRedefinicao("kaike@email.com"));

        assertThat(e.getMessage(), is(MSG_LIMITE));
        assertEquals("hash-codigo", usuarioRedefinicao.getCodigoRedefinicao());
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService, passwordEncoder);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao redefinir a senha de um e-mail inexistente")
    void redefinirSenhaNotFoundError() {
        when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.redefinirSenha(new RedefinirSenhaRequest("x@email.com", "123456", "novaSenha")));

        assertThat(e.getMessage(), is(MSG_CODIGO_INVALIDO));
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao redefinir sem ter solicitado o código")
    void redefinirSenhaWithoutCodeError() {
        usuarioRedefinicao.setCodigoRedefinicao(null);
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioRedefinicao));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.redefinirSenha(new RedefinirSenhaRequest("kaike@email.com", "123456", "novaSenha")));

        assertThat(e.getMessage(), is(MSG_CODIGO_INVALIDO));
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o código de redefinição estiver expirado")
    void redefinirSenhaExpiredError() {
        usuarioRedefinicao.setRedefinicaoExpiraEm(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioRedefinicao));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.redefinirSenha(new RedefinirSenhaRequest("kaike@email.com", "123456", "novaSenha")));

        assertThat(e.getMessage(), is(MSG_CODIGO_INVALIDO));
        assertEquals("hash", usuarioRedefinicao.getSenha());
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Deve incrementar as tentativas quando o código de redefinição estiver errado")
    void redefinirSenhaWrongCodeError() {
        usuarioRedefinicao.setTentativasRedefinicao(1);
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioRedefinicao));
        when(passwordEncoder.matches("000000", "hash-codigo")).thenReturn(false);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.redefinirSenha(new RedefinirSenhaRequest("kaike@email.com", "000000", "novaSenha")));

        assertThat(e.getMessage(), is(MSG_CODIGO_INVALIDO));
        assertEquals(2, usuarioRedefinicao.getTentativasRedefinicao());
        assertEquals("hash-codigo", usuarioRedefinicao.getCodigoRedefinicao());
        assertEquals("hash", usuarioRedefinicao.getSenha());
        verify(usuarioRepository).save(usuarioRedefinicao);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Deve invalidar o código de redefinição ao atingir o máximo de tentativas")
    void redefinirSenhaMaxAttemptsError() {
        usuarioRedefinicao.setTentativasRedefinicao(4);
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioRedefinicao));
        when(passwordEncoder.matches("000000", "hash-codigo")).thenReturn(false);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.redefinirSenha(new RedefinirSenhaRequest("kaike@email.com", "000000", "novaSenha")));

        assertThat(e.getMessage(), is(MSG_CODIGO_INVALIDO));
        assertNull(usuarioRedefinicao.getCodigoRedefinicao());
        assertNull(usuarioRedefinicao.getRedefinicaoExpiraEm());
        assertEquals(0, usuarioRedefinicao.getTentativasRedefinicao());
        assertEquals("hash", usuarioRedefinicao.getSenha());
        verify(usuarioRepository).save(usuarioRedefinicao);
    }

    @Test
    @DisplayName("Deve redefinir a senha e limpar os dados de redefinição")
    void redefinirSenhaSuccess() {
        usuarioRedefinicao.setTentativasRedefinicao(2);
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioRedefinicao));
        when(passwordEncoder.matches("123456", "hash-codigo")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha")).thenReturn("hash-novo");

        authService.redefinirSenha(new RedefinirSenhaRequest("kaike@email.com", "123456", "novaSenha"));

        assertEquals("hash-novo", usuarioRedefinicao.getSenha());
        assertNull(usuarioRedefinicao.getCodigoRedefinicao());
        assertNull(usuarioRedefinicao.getRedefinicaoExpiraEm());
        assertEquals(0, usuarioRedefinicao.getTentativasRedefinicao());
        verify(usuarioRepository).save(usuarioRedefinicao);
    }

    @Test
    @DisplayName("Deve realizar o login e retornar token, expiração, ID do usuário e roles")
    void loginSuccess() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                usuarioVerificado, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_BARBEIRO")));
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioVerificado));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("jwt-token");

        TokenResponse response = authService.login(new LoginRequest("  kaike@email.com  ", "123456"));

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());

        assertEquals("kaike@email.com", captor.getValue().getPrincipal());
        assertEquals("123456", captor.getValue().getCredentials());
        assertEquals("jwt-token", response.token());
        assertEquals(3600000L, response.expirationTime());
        assertEquals(1L, response.userId());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_BARBEIRO"), response.roles());
        verify(tokenProvider).generateToken(auth);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException no login quando o e-mail não estiver cadastrado")
    void loginEmailNotFoundError() {
        when(usuarioRepository.findByEmail("x@email.com")).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> authService.login(new LoginRequest("x@email.com", "123456")));

        assertThat(e.getMessage(), is("E-mail não cadastrado."));
        verifyNoInteractions(authenticationManager, tokenProvider);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException e não gerar token com a senha incorreta")
    void loginInvalidCredentialsError() {
        when(usuarioRepository.findByEmail("kaike@email.com")).thenReturn(Optional.of(usuarioVerificado));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> authService.login(new LoginRequest("kaike@email.com", "errada")));

        assertThat(e.getMessage(), is("Senha incorreta."));
        verifyNoInteractions(tokenProvider);
    }
}