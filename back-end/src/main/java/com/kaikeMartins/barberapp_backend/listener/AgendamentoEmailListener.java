package com.kaikeMartins.barberapp_backend.listener;

import com.kaikeMartins.barberapp_backend.event.ComprovanteCancelamentoEvent;
import com.kaikeMartins.barberapp_backend.event.AgendamentoCriadoEvent;
import com.kaikeMartins.barberapp_backend.event.ClienteDesbloqueadoEvent;
import com.kaikeMartins.barberapp_backend.event.PagamentoAprovadoEvent;
import com.kaikeMartins.barberapp_backend.event.ReembolsoEvent;
import com.kaikeMartins.barberapp_backend.event.VagaFilaEvent;
import com.kaikeMartins.barberapp_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AgendamentoEmailListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void comprovante(AgendamentoCriadoEvent evento) {
        emailService.enviarComprovanteAgendamento(evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void pagamentoAprovado(PagamentoAprovadoEvent evento) {
        emailService.enviarPagamentoAprovado(evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void reembolso(ReembolsoEvent evento) {
        emailService.enviarReembolso(evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void vagaNaFila(VagaFilaEvent evento) {
        emailService.enviarVagaFila(evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cancelamento(ComprovanteCancelamentoEvent evento) {
        emailService.enviarCancelamento(evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void desbloqueio(ClienteDesbloqueadoEvent evento) {
        emailService.enviarDesbloqueio(evento);
    }
}
