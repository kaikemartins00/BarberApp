package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.event.AvisoCancelamento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Async
    public void enviarCodigo(String destino, String codigo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(destino);
            message.setSubject("Código de verificação - BarberApp");

            message.setText(
                    "Olá!\n\n" +
                            "Seu código de verificação é:\n\n" +
                            codigo +
                            "\n\nEste código expira em 5 minutos.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("E-mail de verificação enviado com sucesso para: {}", destino);

        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de verificação para {}. Erro: {}", destino, e.getMessage());
        }
    }

    @Async
    public void enviarCancelamento(AvisoCancelamento aviso) {
        try {
            String motivo = (aviso.motivo() == null || aviso.motivo().isBlank())
                    ? ""
                    : "Motivo: " + aviso.motivo().trim() + "\n\n";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(aviso.email());
            message.setSubject("Atendimento cancelado - BarberApp");

            message.setText(
                    "Olá, " + aviso.clienteNome() + "!\n\n" +
                            "Infelizmente o seu atendimento foi cancelado:\n\n" +
                            aviso.servicoNome() + " com " + aviso.barbeiroNome() + "\n" +
                            aviso.data().format(DATA) + " às " + aviso.horario().format(HORA) + "\n\n" +
                            motivo +
                            "Pedimos desculpas pelo transtorno. Você pode marcar um novo horário pelo app.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("E-mail de cancelamento enviado para: {}", aviso.email());

        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de cancelamento para {}. Erro: {}", aviso.email(), e.getMessage());
        }
    }
}