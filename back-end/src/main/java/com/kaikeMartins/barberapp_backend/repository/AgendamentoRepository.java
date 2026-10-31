package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<AgendamentoEntity, Long> {

    boolean existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
            Long barbeiroId, LocalDate data, LocalTime horario, AgendamentoStatus status);

    boolean existsByServicoId(Long servicoId);

    List<AgendamentoEntity> findByClienteId(Long clienteId);

    List<AgendamentoEntity> findByBarbeiroId(Long barbeiroId);

    List<AgendamentoEntity> findByBarbeiroIdAndData(Long barbeiroId, LocalDate data);

    List<AgendamentoEntity> findByBarbeiroIdAndDataBetweenAndStatusIn(
            Long barbeiroId, LocalDate inicio, LocalDate fim, Collection<AgendamentoStatus> status);
}