package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.FilaEsperaEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.FilaEsperaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FilaEsperaRepository extends JpaRepository<FilaEsperaEntity, Long> {

    boolean existsByAgendamentoIdAndStatus(Long agendamentoId, FilaEsperaStatus status);

    Optional<FilaEsperaEntity> findByIdAndAgendamentoClienteId(Long id, Long clienteId);

    List<FilaEsperaEntity> findByAgendamentoIdAndStatus(Long agendamentoId, FilaEsperaStatus status);

    @Query("""
        SELECT f FROM FilaEsperaEntity f
        JOIN f.agendamento a
        WHERE a.barbeiro.id = :barbeiroId
          AND f.dataPreferida = :data
          AND f.status = :status
        ORDER BY f.criadoEm ASC, f.id ASC
    """)
    List<FilaEsperaEntity> buscarFila(
            @Param("barbeiroId") Long barbeiroId,
            @Param("data") LocalDate data,
            @Param("status") FilaEsperaStatus status
    );

    List<FilaEsperaEntity> findByStatusAndDataPreferidaBefore(FilaEsperaStatus status, LocalDate data);
}
