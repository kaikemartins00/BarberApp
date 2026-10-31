package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.ConversaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversaRepository extends JpaRepository<ConversaEntity, Long> {
    Optional<ConversaEntity> findByClienteIdAndBarbeiroId(Long clienteId, Long barbeiroId);
    List<ConversaEntity> findByClienteId(Long clienteId);
    List<ConversaEntity> findByBarbeiroId(Long barbeiroId);
}