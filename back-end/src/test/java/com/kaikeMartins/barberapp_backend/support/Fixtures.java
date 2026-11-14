package com.kaikeMartins.barberapp_backend.support;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.RoleEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Fixtures {

    public static final String CLIENTE = "ROLE_CLIENTE";
    public static final String BARBEIRO = "ROLE_BARBEIRO";
    public static final String ADMIN = "ROLE_ADMIN";

    private Fixtures() {
    }

    public static UsuarioEntity usuario(Long id, String role) {
        return UsuarioEntity.builder()
                .id(id)
                .email("usuario" + id + "@email.com")
                .senha("123456")
                .verificado(true)
                .roles(Set.of(RoleEntity.builder().name(role).build()))
                .build();
    }

    public static ClienteEntity cliente(Long id) {
        return ClienteEntity.builder()
                .id(id)
                .usuario(usuario(id, CLIENTE))
                .nome("Cliente " + id)
                .telefone("79999999999")
                .build();
    }

    public static BarbeiroEntity barbeiro(Long id) {
        return BarbeiroEntity.builder()
                .id(id)
                .usuario(usuario(id, BARBEIRO))
                .nome("Barbeiro " + id)
                .telefone("79988888888")
                .servicos(new HashSet<>())
                .horariosTrabalho(new HashSet<>())
                .build();
    }

    public static ServicoEntity servico(Long id, double preco) {
        return ServicoEntity.builder()
                .id(id)
                .nome("Servico " + id)
                .preco(preco)
                .build();
    }

    public static AgendamentoEntity agendamento(Long id, ClienteEntity cliente, BarbeiroEntity barbeiro,
                                                ServicoEntity servico, LocalDate data, LocalTime horario,
                                                AgendamentoStatus status) {
        return AgendamentoEntity.builder()
                .id(id)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(data)
                .horario(horario)
                .status(status)
                .build();
    }

    public static void autenticar(UsuarioEntity usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    public static void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    public static List<LocalTime> horarios(int... horas) {
        return java.util.Arrays.stream(horas).mapToObj(h -> LocalTime.of(h, 0)).toList();
    }
}
