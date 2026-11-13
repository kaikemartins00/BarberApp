
package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<PagamentoEntity, Long> {

    Optional<PagamentoEntity> findByAgendamentoId(Long agendamentoId);

    Optional<PagamentoEntity> findByMercadoPagoId(String mercadoPagoId);

    boolean existsByAgendamentoId(Long agendamentoId);

    List<PagamentoEntity> findByReembolsoPendenteTrue();

    List<PagamentoEntity> findByStatusAndMetodo(PagamentoStatus status, PagamentoMetodo metodo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM PagamentoEntity p
        WHERE p.agendamento.id = :agendamentoId
    """)
    Optional<PagamentoEntity> buscarParaAtualizacao(@Param("agendamentoId") Long agendamentoId);
}
