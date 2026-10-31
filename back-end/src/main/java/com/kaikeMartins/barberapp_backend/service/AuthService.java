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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

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

        String codigo = gerarCodigo();
        usuario.setCodigoVerificacao(codigo);
        usuario.setCodigoExpiracao(LocalDateTime.now().plusMinutes(5));
        usuarioRepository.save(usuario);

        emailService.enviarCodigo(email, codigo);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        UsuarioEntity usuario = (UsuarioEntity) authentication.getPrincipal();

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = tokenProvider.generateToken(authentication);

        return new TokenResponse(token, expirationTime, usuario.getId(),  roles);

    }

    private String gerarCodigo() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

}