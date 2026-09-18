package com.kaikeMartins.barberapp_backend.domain.entities;

import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import jakarta.persistence.EntityListeners;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name= "chamados")
public class ChamadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String titulo;
    @Column(nullable = false)
    private String descricao;
    @Column(nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime chamadoDate;
    @Enumerated(EnumType.STRING)
    private ChamadoStatus status;
    @Enumerated(EnumType.STRING)
    private PrioridadeChamado prioridade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id")
    private BarbeiroEntity barbeiro;

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RespostaChamadoEntity> respostas = new ArrayList<>();

}
