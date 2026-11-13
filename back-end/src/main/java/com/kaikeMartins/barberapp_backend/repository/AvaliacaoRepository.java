package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AvaliacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<AvaliacaoEntity,Long> {

    Optional<AvaliacaoEntity> findByAgendamentoId(Long agendamentoId);

    boolean existsByAgendamentoId(Long agendamentoId);

    List<AvaliacaoEntity> findByAgendamentoBarbeiroId(Long barbeiroId);

    List<AvaliacaoEntity> findByAgendamentoClienteId(Long clienteId);
}
