package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarbeiroRepository extends JpaRepository<BarbeiroEntity, Long> {
    Optional<BarbeiroEntity> findByUsuarioEmail(String email);

    Optional<BarbeiroEntity> findByNomeIgnoreCase(String nome);

    List<BarbeiroEntity> findByServicosId(Long servicoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BarbeiroEntity b WHERE b.id = :id")
    Optional<BarbeiroEntity> buscarComBloqueio(
            @Param("id") Long id
    );
}