
package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<AgendamentoEntity, Long> {

    long countByClienteIdAndCanceladoPeloClienteTrueAndCanceladoEmBetween(
            Long clienteId, LocalDateTime inicio, LocalDateTime fim);

    boolean existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
            Long barbeiroId,
            LocalDate data,
            LocalTime horario,
            AgendamentoStatus status
    );

    boolean existsByServicoId(Long servicoId);

    @Query("select a.servico.nome, count(a) from AgendamentoEntity a "
            + "where a.status <> com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus.CANCELADO "
            + "group by a.servico.nome order by count(a) desc")
    List<Object[]> servicosMaisFeitos();

    List<AgendamentoEntity> findByClienteId(Long clienteId);

    List<AgendamentoEntity> findByBarbeiroId(Long barbeiroId);

    List<AgendamentoEntity> findByBarbeiroIdAndData(
            Long barbeiroId,
            LocalDate data
    );

    List<AgendamentoEntity> findByBarbeiroIdAndDataBetweenAndStatusIn(
            Long barbeiroId,
            LocalDate inicio,
            LocalDate fim,
            Collection<AgendamentoStatus> status
    );

    // NOVO: agendamentos futuros do cliente.
    @Query("""
        SELECT a FROM AgendamentoEntity a
        WHERE a.cliente.id = :clienteId
          AND a.status IN :status
          AND (
              a.data > :data
              OR (a.data = :data AND a.horario > :horario)
          )
        ORDER BY a.data, a.horario
    """)
    List<AgendamentoEntity> buscarFuturosDoCliente(
            @Param("clienteId") Long clienteId,
            @Param("status") Collection<AgendamentoStatus> status,
            @Param("data") LocalDate data,
            @Param("horario") LocalTime horario
    );


    @Query("""
        SELECT a.id FROM AgendamentoEntity a
        WHERE a.status IN :status
          AND a.lembreteEnviadoEm IS NULL
          AND (
              a.data > :dataInicio
              OR (a.data = :dataInicio AND a.horario > :horaInicio)
          )
          AND (
              a.data < :dataFim
              OR (a.data = :dataFim AND a.horario <= :horaFim)
          )
    """)
    List<Long> buscarLembretesPendentes(
            @Param("status") Collection<AgendamentoStatus> status,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("horaFim") LocalTime horaFim
    );


    @Modifying
    @Query("""
        UPDATE AgendamentoEntity a
        SET a.lembreteEnviadoEm = :agora
        WHERE a.id = :id
          AND a.lembreteEnviadoEm IS NULL
          AND a.status IN :status
    """)
    int reservarLembrete(
            @Param("id") Long id,
            @Param("agora") LocalDateTime agora,
            @Param("status") Collection<AgendamentoStatus> status
    );
}
