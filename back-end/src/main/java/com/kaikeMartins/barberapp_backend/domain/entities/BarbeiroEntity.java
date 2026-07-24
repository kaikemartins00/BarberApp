package com.kaikeMartins.barberapp_backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "barbeiros")
public class BarbeiroEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(name = "foto_url")
    private String fotoUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "barbeiro_roles",
            joinColumns = @JoinColumn(name = "barbeiro_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<RoleEntity> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "barbeiro_servicos",
            joinColumns = @JoinColumn(name = "barbeiro_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )

    private Set<ServicoEntity> servicos = new HashSet<>();

    @JoinTable(name = "barbeiro_horarios_trabalho", joinColumns = @JoinColumn(name = "barbeiro_id"))
    @Column(name = "horario")
    private Set<LocalTime> horariosTrabalho = new HashSet<>();

    @Column(name = "verificado")
    private boolean verificado = false;

    @Column(name = "codigo_verificacao")
    private String codigoVerificacao;

    @Column(name = "codigo_expiracao")
    private LocalDateTime codigoExpiracao;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return roles; }

    @Override
    public String getPassword() { return senha; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}