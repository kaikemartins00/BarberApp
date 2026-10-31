package com.kaikeMartins.barberapp_backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conversas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "barbeiro_id"}))
public class ConversaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private BarbeiroEntity barbeiro;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean encerrada;
}