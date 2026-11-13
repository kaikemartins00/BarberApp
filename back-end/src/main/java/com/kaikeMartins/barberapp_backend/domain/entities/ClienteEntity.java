package com.kaikeMartins.barberapp_backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "clientes_tb")
public class ClienteEntity {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    @Column(name = "preferencias", columnDefinition = "TEXT")
    private String preferencias;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(name = "desbloqueado_em")
    private LocalDateTime desbloqueadoEm;

    @Column(name = "aceitou_termos_em")
    private LocalDateTime aceitouTermosEm;

    @Column(name = "versao_termos", length = 20)
    private String versaoTermos;

}