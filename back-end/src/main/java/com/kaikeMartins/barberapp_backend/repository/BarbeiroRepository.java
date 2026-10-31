package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarbeiroRepository extends JpaRepository<BarbeiroEntity, Long> {
    Optional<BarbeiroEntity> findByUsuarioEmail(String email);

    Optional<BarbeiroEntity> findByNomeIgnoreCase(String nome);

    List<BarbeiroEntity> findByServicosId(Long servicoId);
}