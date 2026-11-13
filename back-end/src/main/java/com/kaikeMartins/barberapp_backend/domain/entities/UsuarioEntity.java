package com.kaikeMartins.barberapp_backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios_tb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String senha;

    private boolean verificado;
    private String codigoVerificacao;
    private LocalDateTime codigoExpiracao;

    private String codigoRedefinicao;
    private LocalDateTime redefinicaoExpiraEm;

    @Column(nullable = false)
    @Builder.Default
    private int emailsEnviadosHoje = 0;

    private LocalDate dataContagemEmails;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int tentativasRedefinicao;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles;
    }
    @Override public String getPassword() {
        return this.senha;
    }
    @Override public String getUsername() {
        return this.email;
    }
    @Override public boolean isAccountNonExpired() {
        return true;
    }
    @Override public boolean isAccountNonLocked() {
        return true;
    }
    @Override public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override public boolean isEnabled() {
        return this.verificado;
    }
}