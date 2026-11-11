package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.RoleEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.RoleTypeEnum;
import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.RoleRepository;
import com.kaikeMartins.barberapp_backend.repository.UserRepository;
import com.kaikeMartins.barberapp_backend.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int VALIDADE_REDEFINICAO_MINUTOS = 10;
    private static final int INTERVALO_ENTRE_PEDIDOS_SEGUNDOS = 60;
    private static final int MAX_TENTATIVAS_REDEFINICAO = 5;
    private static final String MSG_CODIGO_INVALIDO = "Código inválido ou expirado. Solicite um novo código.";
    private static final int MAX_EMAILS_POR_DIA = 3;

    private static final ZoneId FUSO_BRASIL =
            ZoneId.of("America/Sao_Paulo");

    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final UserRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    @Value("${spring.jwt.expiration}")
    private long expirationTime;

    @Transactional
    public void registerBarbeiro(BarbeiroRequest dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BadRequestException("O email já existe.");
        }

        RoleEntity role = roleRepository.findByName(RoleTypeEnum.ROLE_BARBEIRO.name())
                .orElseGet(() -> roleRepository.save(
                        RoleEntity.builder().name(RoleTypeEnum.ROLE_BARBEIRO.name()).build()));

        UsuarioEntity usuario = usuarioRepository.save(UsuarioEntity.builder()
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .roles(new HashSet<>(Set.of(role)))
                .verificado(true)
                .build());


        barbeiroRepository.save(BarbeiroEntity.builder()
                .usuario(usuario)
                .nome(dto.nome())
                .telefone(dto.telefone())
                .fotoUrl(dto.fotoUrl())
                .build());
    }

    @Transactional
    public void register(CadastroRequest dto) {
        Optional<UsuarioEntity> existente = usuarioRepository.findByEmail(dto.email());

        if (existente.isPresent()) {
            if (existente.get().isVerificado()) {
                throw new BadRequestException("O email já existe.");
            }

            reenviarCodigo(dto.email());
            return;
        }

        RoleEntity role = roleRepository.findByName(RoleTypeEnum.ROLE_CLIENTE.name())
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(RoleTypeEnum.ROLE_CLIENTE.name()).build()));

        String codigo = gerarCodigo();

        UsuarioEntity usuario = usuarioRepository.save(UsuarioEntity.builder()
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .roles(Set.of(role))
                .verificado(false)
                .codigoVerificacao(codigo)
                .codigoExpiracao(LocalDateTime.now().plusMinutes(5))
                .build());

        clienteRepository.save(ClienteEntity.builder()
                .usuario(usuario)
                .nome(dto.nome())
                .telefone(dto.telefone())
                .build());

        verificarLimiteEmail(usuario);

        emailService.enviarCodigo(dto.email(), codigo);
    }

    @Transactional
    public void validarCodigo(ValidarCodigoRequest dto) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        if (usuario.isVerificado()) {
            throw new BadRequestException("Conta já verificada.");
        }

        if (usuario.getCodigoExpiracao().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Código expirado. Solicite um novo.");
        }

        if (!usuario.getCodigoVerificacao().equals(dto.codigo())) {
            throw new BadRequestException("Código inválido.");
        }

        usuario.setVerificado(true);
        usuario.setCodigoVerificacao(null);
        usuario.setCodigoExpiracao(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void reenviarCodigo(String email) {

        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        if (usuario.isVerificado()) {
            throw new BadRequestException("Conta já verificada.");
        }

        verificarLimiteEmail(usuario);

        String codigo = gerarCodigo();

        usuario.setCodigoVerificacao(codigo);
        usuario.setCodigoExpiracao(LocalDateTime.now().plusMinutes(5));

        usuarioRepository.save(usuario);

        emailService.enviarCodigo(email, codigo);
    }

    @Transactional
    public void solicitarRedefinicao(String email) {
        Optional<UsuarioEntity> encontrado = usuarioRepository.findByEmail(email.trim());
        if (encontrado.isEmpty()) {
            return;
        }

        UsuarioEntity usuario = encontrado.get();
        LocalDateTime agora = LocalDateTime.now();

        if (usuario.getRedefinicaoExpiraEm() != null) {
            LocalDateTime emitidoEm = usuario.getRedefinicaoExpiraEm().minusMinutes(VALIDADE_REDEFINICAO_MINUTOS);
            if (emitidoEm.plusSeconds(INTERVALO_ENTRE_PEDIDOS_SEGUNDOS).isAfter(agora)) {
                return;
            }
        }

        verificarLimiteEmail(usuario);

        String codigo = gerarCodigo();
        usuario.setCodigoRedefinicao(passwordEncoder.encode(codigo));
        usuario.setRedefinicaoExpiraEm(agora.plusMinutes(VALIDADE_REDEFINICAO_MINUTOS));
        usuario.setTentativasRedefinicao(0);
        usuarioRepository.save(usuario);

        emailService.enviarCodigoRedefinicao(usuario.getEmail(), codigo);
    }

    private void verificarLimiteEmail(UsuarioEntity usuario) {

        LocalDate hoje = LocalDate.now(FUSO_BRASIL);

        if (usuario.getDataContagemEmails() == null
                || !usuario.getDataContagemEmails().equals(hoje)) {

            usuario.setEmailsEnviadosHoje(0);
            usuario.setDataContagemEmails(hoje);
        }

        if (usuario.getEmailsEnviadosHoje() >= MAX_EMAILS_POR_DIA) {
            throw new BadRequestException(
                    "Você atingiu o limite de 3 e-mails por dia. Tente novamente amanhã."
            );
        }

        usuario.setEmailsEnviadosHoje(
                usuario.getEmailsEnviadosHoje() + 1
        );

        usuarioRepository.save(usuario);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void redefinirSenha(RedefinirSenhaRequest dto) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(dto.email().trim())
                .orElseThrow(() -> new BadRequestException(MSG_CODIGO_INVALIDO));

        if (usuario.getCodigoRedefinicao() == null
                || usuario.getRedefinicaoExpiraEm() == null
                || usuario.getRedefinicaoExpiraEm().isBefore(LocalDateTime.now())) {
            throw new BadRequestException(MSG_CODIGO_INVALIDO);
        }

        if (!passwordEncoder.matches(dto.codigo().trim(), usuario.getCodigoRedefinicao())) {
            int tentativas = usuario.getTentativasRedefinicao() + 1;

            if (tentativas >= MAX_TENTATIVAS_REDEFINICAO) {
                limparRedefinicao(usuario);
            } else {
                usuario.setTentativasRedefinicao(tentativas);
            }
            usuarioRepository.save(usuario);

            throw new BadRequestException(MSG_CODIGO_INVALIDO);
        }

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        limparRedefinicao(usuario);
        usuarioRepository.save(usuario);
    }

    private void limparRedefinicao(UsuarioEntity usuario) {
        usuario.setCodigoRedefinicao(null);
        usuario.setRedefinicaoExpiraEm(null);
        usuario.setTentativasRedefinicao(0);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {

        UsuarioEntity usuario = usuarioRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new NotFoundException("E-mail não cadastrado."));

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            usuario.getEmail(),
                            request.senha()
                    )
            );
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new BadRequestException("Senha incorreta.");
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = tokenProvider.generateToken(authentication);

        return new TokenResponse(token, expirationTime, usuario.getId(), roles);
    }

    private String gerarCodigo() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

}