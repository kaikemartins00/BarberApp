package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.RoleEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.RoleTypeEnum;
import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.RoleRepository;
import com.kaikeMartins.barberapp_backend.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    @Value("${spring.jwt.expiration}")
    private long expirationTime;

    public void registerBarbeiro(BarbeiroRequest dto) {
        if (barbeiroRepository.existsByEmail(dto.email())) {
            throw new BadRequestException("O email já existe.");
        }

        RoleEntity role = roleRepository.findByName(RoleTypeEnum.ROLE_BARBEIRO.name())
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(RoleTypeEnum.ROLE_BARBEIRO.name()).build()));

        barbeiroRepository.save(BarbeiroEntity.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .fotoUrl(dto.fotoUrl())
                .roles(Set.of(role))
                .build());
    }

    public void register(CadastroRequest dto) {
        Optional<ClienteEntity> existente = clienteRepository.findByEmail(dto.email());

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

        ClienteEntity cliente = ClienteEntity.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .telefone(dto.telefone())
                .roles(Set.of(role))
                .verificado(false)
                .codigoVerificacao(codigo)
                .codigoExpiracao(LocalDateTime.now().plusMinutes(5))
                .build();

        clienteRepository.save(cliente);

        emailService.enviarCodigo(dto.email(), codigo);
    }

    private String gerarCodigo() {
        int codigo = new java.util.Random().nextInt(900000) + 100000; // sempre 6 dígitos
        return String.valueOf(codigo);
    }

    public void validarCodigo(ValidarCodigoRequest dto) {
        ClienteEntity cliente = clienteRepository.findByEmail(dto.email())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        if (cliente.isVerificado()) {
            throw new BadRequestException("Conta já verificada.");
        }

        if (cliente.getCodigoExpiracao().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Código expirado. Solicite um novo.");
        }

        if (!cliente.getCodigoVerificacao().equals(dto.codigo())) {
            throw new BadRequestException("Código inválido.");
        }

        cliente.setVerificado(true);
        cliente.setCodigoVerificacao(null);
        cliente.setCodigoExpiracao(null);
        clienteRepository.save(cliente);
    }

    public void reenviarCodigo(String email) {
        ClienteEntity cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        if (cliente.isVerificado()) {
            throw new BadRequestException("Conta já verificada.");
        }

        String codigo = gerarCodigo();
        cliente.setCodigoVerificacao(codigo);
        cliente.setCodigoExpiracao(LocalDateTime.now().plusMinutes(5));
        clienteRepository.save(cliente);

        emailService.enviarCodigo(email, codigo);
    }

    public TokenResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.senha()));

            Object principal = authentication.getPrincipal();

            if (principal instanceof ClienteEntity cliente && !cliente.isVerificado()) {
                throw new BadRequestException("Conta não verificada. Confira seu email.");
            }

            if (principal instanceof BarbeiroEntity barbeiro && !barbeiro.isVerificado()) {
                throw new BadRequestException("Conta não verificada. Confira seu email.");
            }

            String token = tokenProvider.generateToken(authentication);

            return new TokenResponse(token, expirationTime);

        } catch (BadCredentialsException e) {
            throw new BadRequestException("invalid credentials");
        }
    }

}