package com.kaikeMartins.barberapp_backend.domain.entities;

import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos_tb")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "agendamento_id",
            nullable = false,
            unique = true
    )
    private AgendamentoEntity agendamento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PagamentoMetodo metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PagamentoStatus status;

    @Column(name = "mercado_pago_id", unique = true)
    private String mercadoPagoId;

    @Column(name = "mercado_pago_qr", columnDefinition = "TEXT")
    private String mercadoPagoQr;

    @Column(name = "mercado_pago_qr_base64", columnDefinition = "TEXT")
    private String mercadoPagoQrBase64;

    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    @Column(name = "reembolsado_em")
    private LocalDateTime reembolsadoEm;

    @Column(name = "reembolso_pendente")
    private Boolean reembolsoPendente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmado_por")
    private UsuarioEntity confirmadoPor;
}
