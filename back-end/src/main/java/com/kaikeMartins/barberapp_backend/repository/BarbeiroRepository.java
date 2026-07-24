package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BarbeiroRepository extends JpaRepository<BarbeiroEntity, Long> {
    Optional<BarbeiroEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
