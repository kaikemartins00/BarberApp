package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.FinanceiroResumoResponse;
import com.kaikeMartins.barberapp_backend.repository.FinanceiroConsultaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

    private final FinanceiroConsultaRepository repository;

    @Transactional(readOnly = true)
    public FinanceiroResumoResponse meu() {
        UsuarioEntity usuario = (UsuarioEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return resumo(repository.findByStatusAndAgendamentoStatusNotAndAgendamentoBarbeiroId(PagamentoStatus.PAGO, AgendamentoStatus.CANCELADO, usuario.getId()));
    }

    @Transactional(readOnly = true)
    public FinanceiroResumoResponse geral() {

        return resumo(repository.findByStatusAndAgendamentoStatusNot(PagamentoStatus.PAGO, AgendamentoStatus.CANCELADO));
    }

    private FinanceiroResumoResponse resumo(List<PagamentoEntity> pagamentos) {

        BigDecimal bruto = pagamentos.stream().map(PagamentoEntity::getValor).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal loja = bruto.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
        return new FinanceiroResumoResponse(bruto, loja, bruto.subtract(loja), pagamentos.size());
    }

}
