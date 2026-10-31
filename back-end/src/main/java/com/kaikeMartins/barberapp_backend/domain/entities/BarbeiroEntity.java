package com.kaikeMartins.barberapp_backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "barbeiros")
public class BarbeiroEntity {

    @Id
    private Long id; // Remove o @GeneratedValue!

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    @Column(name = "foto_url")
    private String fotoUrl;

    @ManyToMany
    @JoinTable(
            name = "barbeiro_servicos",
            joinColumns = @JoinColumn(name = "barbeiro_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    @Builder.Default
    private Set<ServicoEntity> servicos = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "barbeiro_agenda", joinColumns = @JoinColumn(name = "barbeiro_id"))
    @Column(name = "data_hora")
    @Builder.Default
    private Set<LocalDateTime> horariosTrabalho = new HashSet<>();

}