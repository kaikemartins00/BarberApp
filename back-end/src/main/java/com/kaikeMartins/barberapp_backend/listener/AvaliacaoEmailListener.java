package com.kaikeMartins.barberapp_backend.listener;

import com.kaikeMartins.barberapp_backend.event.AvaliacaoSolicitadaEvent;
import com.kaikeMartins.barberapp_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AvaliacaoEmailListener {

    private final EmailService emailService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void enviar(AvaliacaoSolicitadaEvent evento) {
        emailService.enviarConviteAvaliacao(
                evento.emailCliente(),
                evento.nomeCliente()
        );
    }
}
