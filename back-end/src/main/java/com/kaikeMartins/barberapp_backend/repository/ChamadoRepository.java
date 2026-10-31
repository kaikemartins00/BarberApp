package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.ChamadoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChamadoRepository extends JpaRepository<ChamadoEntity, Long> {
    List<ChamadoEntity> findAllByOrderByChamadoDateDesc();
    List<ChamadoEntity> findByClienteIdOrderByChamadoDateDesc(Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChamadoEntity c where c.id = :id")
    Optional<ChamadoEntity> buscarParaAtualizar(@Param("id") Long id);
}