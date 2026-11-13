package com.kaikeMartins.barberapp_backend.listener;

import com.kaikeMartins.barberapp_backend.event.ChamadoNotificacaoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoEmailListener {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAtualizarChamado(ChamadoNotificacaoEvent e) {
        String protocolo = "#" + e.chamadoId();
        String assunto;
        String corpo;

        switch (e.tipo()) {
            case ABERTO -> {
                assunto = "Chamado " + protocolo + " aberto com sucesso - BarberApp";
                corpo = "Recebemos o seu chamado e nossa equipe já foi avisada.\n\n" +
                        "Protocolo: " + protocolo + "\n" +
                        "Assunto: " + e.titulo() + "\n" +
                        "O que você nos contou: " + e.texto() + "\n\n" +
                        "O que acontece agora: assim que alguém começar a atender, você recebe um novo e-mail. " +
                        "Você também pode acompanhar tudo em \"Problemas\", dentro do BarberApp.";
            }
            case ASSUMIDO -> {
                assunto = "Seu chamado " + protocolo + " está sendo atendido - BarberApp";
                corpo = "Boas notícias: " + e.atendente() + " começou a atender o seu chamado.\n\n" +
                        "Protocolo: " + protocolo + "\n" +
                        "Assunto: " + e.titulo() + "\n\n" +
                        "Em breve você recebe a resposta por aqui.";
            }
            case RESPONDIDO -> {
                assunto = "Resposta ao seu chamado " + protocolo + " - BarberApp";
                corpo = e.atendente() + " respondeu ao seu chamado:\n\n" +
                        "\"" + e.texto() + "\"\n\n" +
                        "Protocolo: " + protocolo + "\n" +
                        "Assunto: " + e.titulo() + "\n\n" +
                        "Você pode ver a resposta completa em \"Problemas\", dentro do BarberApp.";
            }
            case FINALIZADO -> {
                assunto = "Chamado " + protocolo + " finalizado - BarberApp";
                corpo = "O seu chamado foi finalizado por " + e.atendente() + ".\n\n" +
                        "Protocolo: " + protocolo + "\n" +
                        "Assunto: " + e.titulo() + "\n\n" +
                        "Se o problema continuar, é só abrir um novo chamado em \"Problemas\". " +
                        "Obrigado por nos ajudar a melhorar o BarberApp.";
            }
            default -> {
                return;
            }
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(e.email());
            message.setSubject(assunto);
            message.setText("Olá, " + e.clienteNome() + "!\n\n" + corpo + "\n\nEquipe BarberApp.");
            mailSender.send(message);
            log.info("E-mail de chamado ({}) enviado para: {}", e.tipo(), e.email());
        } catch (MailException ex) {
            log.error("Falha ao enviar e-mail de chamado para {}", e.email(), ex);
        }
    }
}
