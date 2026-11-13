package com.kaikeMartins.barberapp_backend.repository;

import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FinanceiroConsultaRepository extends JpaRepository<PagamentoEntity, Long> {

    List<PagamentoEntity> findByStatusAndAgendamentoStatusNot(PagamentoStatus status, AgendamentoStatus agendamentoStatus);

    List<PagamentoEntity> findByStatusAndAgendamentoStatusNotAndAgendamentoBarbeiroId(PagamentoStatus status, AgendamentoStatus agendamentoStatus, Long barbeiroId);
}
