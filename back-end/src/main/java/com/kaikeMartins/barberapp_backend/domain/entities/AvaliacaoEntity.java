package com.kaikeMartins.barberapp_backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="avaliacoes_tb", uniqueConstraints=@UniqueConstraint(name="uk_avaliacao_agendamento",columnNames="agendamento_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvaliacaoEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="agendamento_id",nullable=false)
    private AgendamentoEntity agendamento;

    @Column(nullable=false)
    private int nota;

    @Column(length=500)
    private String comentario;

    @Column(length=250)
    private String tags;

    @Column(nullable=false)
    private LocalDateTime criadaEm;
}
