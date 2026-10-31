package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.MensagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MensagemRepository extends JpaRepository<MensagemEntity, Long> {

    List<MensagemEntity> findByConversaIdAndIdGreaterThanOrderByIdAsc(Long conversaId, Long depoisDe);

    Optional<MensagemEntity> findFirstByConversaIdOrderByIdDesc(Long conversaId);

    long countByConversaIdAndRemetenteIdNotAndLidaFalse(Long conversaId, Long remetenteId);

    @Modifying
    @Query("update MensagemEntity m set m.lida = true "
            + "where m.conversa.id = :conversaId and m.remetente.id <> :usuarioId and m.lida = false")
    int marcarComoLidas(@Param("conversaId") Long conversaId, @Param("usuarioId") Long usuarioId);
}