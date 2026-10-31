package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.event.AvisoCancelamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    private AvisoCancelamento aviso;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "remetente", "app@barberapp.com");

        aviso = new AvisoCancelamento("kaike@email.com", "Kaike", "Joao", "Corte",
                LocalDate.of(2027, 3, 12), LocalTime.of(14, 0), "  Barbeiro doente  ");
    }

    @Test
    @DisplayName("Should send verification code email")
    void enviarCodigoSuccess() {

        emailService.enviarCodigo("kaike@email.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage email = captor.getValue();

        assertEquals("app@barberapp.com", email.getFrom());
        assertArrayEquals(new String[]{"kaike@email.com"}, email.getTo());
        assertEquals("Código de verificação - BarberApp", email.getSubject());
        assertTrue(email.getText().contains("123456"));
        assertTrue(email.getText().contains("5 minutos"));
    }

    @Test
    @DisplayName("Should not throw when sending verification code fails")
    void enviarCodigoMailError() {

        doThrow(new MailSendException("smtp fora")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.enviarCodigo("kaike@email.com", "123456"));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send cancellation email with date, time and reason")
    void enviarCancelamentoSuccess() {

        emailService.enviarCancelamento(aviso);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage email = captor.getValue();

        assertEquals("app@barberapp.com", email.getFrom());
        assertArrayEquals(new String[]{"kaike@email.com"}, email.getTo());
        assertEquals("Atendimento cancelado - BarberApp", email.getSubject());
        assertTrue(email.getText().contains("Olá, Kaike!"));
        assertTrue(email.getText().contains("Corte com Joao"));
        assertTrue(email.getText().contains("12/03/2027 às 14:00"));
        assertTrue(email.getText().contains("Motivo: Barbeiro doente"));
    }

    @Test
    @DisplayName("Should not include reason line when reason is blank")
    void enviarCancelamentoBlankReasonSuccess() {

        AvisoCancelamento semMotivo = new AvisoCancelamento("kaike@email.com", "Kaike", "Joao", "Corte",
                LocalDate.of(2027, 3, 12), LocalTime.of(14, 0), "   ");

        emailService.enviarCancelamento(semMotivo);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        assertFalse(captor.getValue().getText().contains("Motivo:"));
    }

    @Test
    @DisplayName("Should not include reason line when reason is null")
    void enviarCancelamentoNullReasonSuccess() {

        AvisoCancelamento semMotivo = new AvisoCancelamento("kaike@email.com", "Kaike", "Joao", "Corte",
                LocalDate.of(2027, 3, 12), LocalTime.of(14, 0), null);

        emailService.enviarCancelamento(semMotivo);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        assertFalse(captor.getValue().getText().contains("Motivo:"));
    }

    @Test
    @DisplayName("Should not throw when sending cancellation fails")
    void enviarCancelamentoMailError() {

        doThrow(new MailSendException("smtp fora")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.enviarCancelamento(aviso));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}