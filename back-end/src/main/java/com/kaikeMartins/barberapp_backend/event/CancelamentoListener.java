package com.kaikeMartins.barberapp_backend.event;

import com.kaikeMartins.barberapp_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CancelamentoListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoCancelar(AgendamentosCanceladosEvent evento) {
        evento.avisos().forEach(emailService::enviarCancelamento);
    }
}