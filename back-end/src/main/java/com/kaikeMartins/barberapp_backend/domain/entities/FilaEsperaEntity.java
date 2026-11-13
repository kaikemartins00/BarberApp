package com.kaikeMartins.barberapp_backend.domain.entities;

import com.kaikeMartins.barberapp_backend.domain.enums.FilaEsperaStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "fila_espera_tb")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilaEsperaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private AgendamentoEntity agendamento;

    @Column(name = "data_preferida", nullable = false)
    private LocalDate dataPreferida;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "fila_espera_horarios_tb",
            joinColumns = @JoinColumn(name = "fila_id")
    )
    @Column(name = "horario", nullable = false)
    @Builder.Default
    private Set<LocalTime> horariosAceitos = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FilaEsperaStatus status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atendido_em")
    private LocalDateTime atendidoEm;
}
