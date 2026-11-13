package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.CheckoutRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final AgendamentoService agendamentoService;
    private final PagamentoService pagamentoService;

    @Transactional
    public AgendamentoResponse finalizar(CheckoutRequest request) {
        if (request.metodo() != PagamentoMetodo.PIX && request.metodo() != PagamentoMetodo.PRESENCIAL) {
            throw new BadRequestException("Método de pagamento inválido.");
        }
        AgendamentoResponse agendamento = agendamentoService.create(request.agendamento());
        pagamentoService.escolherMetodo(agendamento.id(), request.metodo());
        if (request.metodo() == PagamentoMetodo.PRESENCIAL) {
            pagamentoService.confirmarReservaPresencial(agendamento.id());
        }
        return agendamento;
    }
}
