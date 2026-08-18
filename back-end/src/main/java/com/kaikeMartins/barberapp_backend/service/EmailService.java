package com.kaikeMartins.barberapp_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Async
    public void enviarCodigo(String destino, String codigo) {

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
    }

}
