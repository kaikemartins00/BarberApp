package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<AgendamentoEntity, Long> {

    boolean existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
            Long barbeiroId, LocalDate data, LocalTime horario, String status
    );

    List<AgendamentoEntity> findByClienteId(Long clienteId);

    List<AgendamentoEntity> findByBarbeiroIdAndData(Long barbeiroId, LocalDate data);
}
