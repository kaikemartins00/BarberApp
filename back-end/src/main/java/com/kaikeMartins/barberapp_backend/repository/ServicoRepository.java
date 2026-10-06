package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServicoRepository extends JpaRepository<ServicoEntity, Long> {
    Optional<ServicoEntity> findByNomeIgnoreCase(String nome);
}
