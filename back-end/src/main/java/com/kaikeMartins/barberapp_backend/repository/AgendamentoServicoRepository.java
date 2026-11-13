package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoServicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgendamentoServicoRepository extends JpaRepository<AgendamentoServicoEntity, Long> {

    List<AgendamentoServicoEntity> findByAgendamentoIdOrderByIdAsc(Long agendamentoId);

    Optional<AgendamentoServicoEntity> findByIdAndAgendamentoId(Long id, Long agendamentoId);
}
