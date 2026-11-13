package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PixConciliacaoJob {

    private final PagamentoRepository repository;
    private final PixService pixService;

    @Scheduled(initialDelay = 30000, fixedDelay = 20000)
    public void conciliar() {
        repository.findByStatusAndMetodo(PagamentoStatus.AGUARDANDO_CONFIRMACAO, PagamentoMetodo.PIX)
                .stream()
                .map(PagamentoEntity::getMercadoPagoId)
                .filter(Objects::nonNull)
                .forEach(id -> {
                    try {
                        pixService.sincronizar(id);
                    } catch (Exception e) {
                        log.warn("Não foi possível conciliar o Pix {}: {}", id, e.getMessage());
                    }
                });
    }
}
