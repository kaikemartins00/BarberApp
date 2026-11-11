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

        aviso = avisoComMotivo("  Barbeiro doente  ");
    }

    private AvisoCancelamento avisoComMotivo(String motivo) {
        return new AvisoCancelamento("kaike@email.com", "Kaike", "Joao", "Corte",
                LocalDate.of(2027, 3, 12), LocalTime.of(14, 0), motivo);
    }

    private SimpleMailMessage capturarEmail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Deve enviar o e-mail com o código de verificação")
    void enviarCodigoSuccess() {
        emailService.enviarCodigo("kaike@email.com", "123456");

        SimpleMailMessage email = capturarEmail();

        assertEquals("app@barberapp.com", email.getFrom());
        assertArrayEquals(new String[]{"kaike@email.com"}, email.getTo());
        assertEquals("Código de verificação - BarberApp", email.getSubject());
        assertTrue(email.getText().contains("123456"));
        assertTrue(email.getText().contains("5 minutos"));
        assertTrue(email.getText().contains("Equipe BarberApp."));
    }

    @Test
    @DisplayName("Não deve lançar exceção quando o envio do código falhar")
    void enviarCodigoMailError() {
        doThrow(new MailSendException("smtp fora")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.enviarCodigo("kaike@email.com", "123456"));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve enviar o e-mail com o código de redefinição de senha")
    void enviarCodigoRedefinicaoSuccess() {
        emailService.enviarCodigoRedefinicao("kaike@email.com", "654321");

        SimpleMailMessage email = capturarEmail();

        assertEquals("app@barberapp.com", email.getFrom());
        assertArrayEquals(new String[]{"kaike@email.com"}, email.getTo());
        assertEquals("Redefinição de senha - BarberApp", email.getSubject());
        assertTrue(email.getText().contains("654321"));
        assertTrue(email.getText().contains("10 minutos"));
        assertTrue(email.getText().contains("sua senha continua a mesma"));
        assertTrue(email.getText().contains("Equipe BarberApp."));
    }

    @Test
    @DisplayName("Não deve lançar exceção quando o envio do código de redefinição falhar")
    void enviarCodigoRedefinicaoMailError() {
        doThrow(new MailSendException("smtp fora")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.enviarCodigoRedefinicao("kaike@email.com", "654321"));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve enviar o e-mail de cancelamento com data, hora e motivo sem espaços nas pontas")
    void enviarCancelamentoSuccess() {
        emailService.enviarCancelamento(aviso);

        SimpleMailMessage email = capturarEmail();

        assertEquals("app@barberapp.com", email.getFrom());
        assertArrayEquals(new String[]{"kaike@email.com"}, email.getTo());
        assertEquals("Atendimento cancelado - BarberApp", email.getSubject());
        assertTrue(email.getText().contains("Olá, Kaike!"));
        assertTrue(email.getText().contains("Corte com Joao"));
        assertTrue(email.getText().contains("12/03/2027 às 14:00"));
        assertTrue(email.getText().contains("Motivo: Barbeiro doente\n\n"));
        assertTrue(email.getText().contains("Pedimos desculpas pelo transtorno."));
        assertTrue(email.getText().contains("Equipe BarberApp."));
    }

    @Test
    @DisplayName("Não deve incluir a linha de motivo quando o motivo estiver em branco")
    void enviarCancelamentoBlankReasonSuccess() {
        emailService.enviarCancelamento(avisoComMotivo("   "));

        assertFalse(capturarEmail().getText().contains("Motivo:"));
    }

    @Test
    @DisplayName("Não deve incluir a linha de motivo quando o motivo for nulo")
    void enviarCancelamentoNullReasonSuccess() {
        emailService.enviarCancelamento(avisoComMotivo(null));

        assertFalse(capturarEmail().getText().contains("Motivo:"));
    }

    @Test
    @DisplayName("Não deve lançar exceção quando o envio do cancelamento falhar")
    void enviarCancelamentoMailError() {
        doThrow(new MailSendException("smtp fora")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.enviarCancelamento(aviso));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}