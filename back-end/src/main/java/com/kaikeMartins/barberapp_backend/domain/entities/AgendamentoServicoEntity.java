package com.kaikeMartins.barberapp_backend.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "agendamento_servicos_tb")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoServicoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private AgendamentoEntity agendamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private ServicoEntity servico;

    @Column(name = "valor_cobrado",
            nullable = false, precision = 10, scale = 2)
    private BigDecimal valorCobrado;

    @Column(nullable = false)
    private boolean extra;
}
