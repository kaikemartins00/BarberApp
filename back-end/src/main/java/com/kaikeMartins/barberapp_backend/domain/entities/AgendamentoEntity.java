package com.kaikeMartins.barberapp_backend.domain.entities;

import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "agendamentos_tb")
public class AgendamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @ManyToOne
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private BarbeiroEntity barbeiro;

    @ManyToOne
    @JoinColumn(name = "servico_id", nullable = false)
    private ServicoEntity servico;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime horario;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgendamentoStatus status;

    @Column(name = "lembrete_enviado_em")
    private LocalDateTime lembreteEnviadoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_pelo_cliente")
    private Boolean canceladoPeloCliente;
}
