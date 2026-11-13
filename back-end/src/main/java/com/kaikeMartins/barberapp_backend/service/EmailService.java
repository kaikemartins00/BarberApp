
package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.event.ComprovanteCancelamentoEvent;
import com.kaikeMartins.barberapp_backend.event.AgendamentoCriadoEvent;
import com.kaikeMartins.barberapp_backend.event.ClienteDesbloqueadoEvent;
import com.kaikeMartins.barberapp_backend.event.AvisoCancelamento;
import com.kaikeMartins.barberapp_backend.event.PagamentoAprovadoEvent;
import com.kaikeMartins.barberapp_backend.event.ReembolsoEvent;
import com.kaikeMartins.barberapp_backend.event.VagaFilaEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm");

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
            log.info("E-mail de verificação enviado para: {}", destino);

        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de verificação para {}", destino, e);
        }
    }

    @Async
    public void enviarCodigoRedefinicao(String destino, String codigo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(destino);
            message.setSubject("Redefinição de senha - BarberApp");

            message.setText(
                    "Olá!\n\n" +
                            "Recebemos um pedido para redefinir a senha da sua conta.\n" +
                            "Seu código é:\n\n" +
                            codigo +
                            "\n\nEste código expira em 10 minutos.\n\n" +
                            "Se você não pediu a redefinição, ignore este e-mail: " +
                            "sua senha continua a mesma.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("E-mail de redefinição enviado para: {}", destino);

        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de redefinição para {}", destino, e);
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
                            aviso.data().format(DATA) + " às " +
                            aviso.horario().format(HORA) + "\n\n" +
                            motivo +
                            "Pedimos desculpas pelo transtorno. " +
                            "Você pode marcar um novo horário pelo app.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("E-mail de cancelamento enviado para: {}", aviso.email());

        } catch (MailException e) {
            log.error("Falha ao enviar cancelamento para {}", aviso.email(), e);
        }
    }

    @Async
    public void enviarConfirmacaoAgendamento(
            String destino,
            String clienteNome,
            String barbeiroNome,
            String servicoNome,
            LocalDate data,
            LocalTime horario
    ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(destino);
            message.setSubject("Agendamento confirmado - BarberApp");

            message.setText(
                    "Olá, " + clienteNome + "!\n\n" +
                            "Seu agendamento foi realizado com sucesso!\n\n" +
                            "Serviço: " + servicoNome + "\n" +
                            "Barbeiro: " + barbeiroNome + "\n" +
                            "Data: " + data.format(DATA) + "\n" +
                            "Horário: " + horario.format(HORA) + "\n\n" +
                            "Você pode acompanhar ou cancelar seu agendamento " +
                            "pelo BarberApp.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("Confirmação enviada para: {}", destino);

        } catch (MailException e) {
            log.error("Falha ao enviar confirmação para {}", destino, e);
        }
    }

    @Async
    public void enviarComprovanteAgendamento(AgendamentoCriadoEvent e) {
        try {
            String valor = e.preco() == null
                    ? ""
                    : "Valor: " + String.format(java.util.Locale.of("pt", "BR"), "R$ %.2f", e.preco()) + "\n";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(e.email());
            message.setSubject("Comprovante de agendamento - BarberApp");

            message.setText(
                    "Olá, " + e.clienteNome() + "!\n\n" +
                            "Seu agendamento foi realizado com sucesso. Segue o comprovante:\n\n" +
                            "Protocolo: #" + e.agendamentoId() + "\n" +
                            "Serviço: " + e.servicoNome() + "\n" +
                            "Barbeiro: " + e.barbeiroNome() + "\n" +
                            "Data: " + e.data().format(DATA) + "\n" +
                            "Horário: " + e.horario().format(HORA) + "\n" +
                            valor + "\n" +
                            "Você pode acompanhar, pagar ou cancelar seu agendamento " +
                            "pelo BarberApp.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("Comprovante enviado para: {}", e.email());

        } catch (MailException ex) {
            log.error("Falha ao enviar comprovante para {}", e.email(), ex);
        }
    }

    @Async
    public void enviarPagamentoAprovado(PagamentoAprovadoEvent e) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(e.email());
            message.setSubject("Pagamento aprovado com sucesso - BarberApp");

            message.setText(
                    "Olá, " + e.clienteNome() + "!\n\n" +
                            "Pagamento aprovado com sucesso! Seu Pix foi confirmado e o seu horário está garantido.\n\n" +
                            "Protocolo: #" + e.agendamentoId() + "\n" +
                            "Serviço: " + e.servicoNome() + "\n" +
                            "Barbeiro: " + e.barbeiroNome() + "\n" +
                            "Data: " + e.data().format(DATA) + "\n" +
                            "Horário: " + e.horario().format(HORA) + "\n" +
                            "Valor pago: " + String.format(java.util.Locale.of("pt", "BR"), "R$ %.2f", e.valor()) + "\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("Pagamento aprovado enviado para: {}", e.email());

        } catch (MailException ex) {
            log.error("Falha ao enviar aprovação de pagamento para {}", e.email(), ex);
        }
    }

    @Async
    public void enviarReembolso(ReembolsoEvent e) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(e.email());
            message.setSubject("Comprovante de reembolso - BarberApp");

            message.setText(
                    "Olá, " + e.clienteNome() + "!\n\n" +
                            "Seu reembolso foi realizado com sucesso. Segue o comprovante:\n\n" +
                            "Protocolo do agendamento: #" + e.agendamentoId() + "\n" +
                            "Serviço: " + e.servicoNome() + "\n" +
                            "Data do atendimento: " + e.data().format(DATA) + " às " + e.horario().format(HORA) + "\n" +
                            "Valor reembolsado: " + String.format(java.util.Locale.of("pt", "BR"), "R$ %.2f", e.valor()) + "\n" +
                            "Forma: Pix (devolvido à conta de origem)\n" +
                            "Reembolso efetuado em: " + e.efetuadoEm().format(DATA) + " às " + e.efetuadoEm().format(HORA) + "\n" +
                            "Código da transação: " + e.codigoTransacao() + "\n\n" +
                            "O valor volta para a conta de onde saiu o Pix. " +
                            "Dependendo do banco, pode levar alguns instantes para aparecer.\n\n" +
                            "Você pode marcar um novo horário pelo BarberApp quando quiser.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("Aviso de reembolso enviado para: {}", e.email());

        } catch (MailException ex) {
            log.error("Falha ao enviar aviso de reembolso para {}", e.email(), ex);
        }
    }

    @Async
    public void enviarCancelamento(ComprovanteCancelamentoEvent e) {
        try {
            StringBuilder texto = new StringBuilder();
            texto.append("Olá, ").append(e.clienteNome()).append("!\n\n");

            if (e.pelaBarbearia()) {
                texto.append("A barbearia precisou cancelar o seu horário e pedimos desculpas pelo transtorno. ")
                        .append("Segue o comprovante:\n\n");
            } else {
                texto.append("Seu agendamento foi cancelado. Segue o comprovante:\n\n");
            }

            texto.append("Protocolo do agendamento: #").append(e.agendamentoId()).append("\n")
                    .append("Serviço: ").append(e.servicoNome()).append("\n")
                    .append("Barbeiro: ").append(e.barbeiroNome()).append("\n")
                    .append("Horário cancelado: ").append(e.data().format(DATA))
                    .append(" às ").append(e.horario().format(HORA)).append("\n\n");

            texto.append("REEMBOLSO\n");
            if (e.pelaBarbearia()) {
                texto.append("Se você já tinha pago por Pix, o valor é devolvido por inteiro e automaticamente. ")
                        .append("Você recebe um comprovante de reembolso à parte.\n\n")
                        .append("Esse cancelamento não conta no seu limite mensal.\n\n");
            } else if (e.comAntecedencia()) {
                texto.append("Se você já tinha pago por Pix, o reembolso é feito automaticamente e você recebe ")
                        .append("um comprovante à parte.\n\n");
            } else {
                texto.append("Como o cancelamento foi feito com menos de 2 horas de antecedência, ")
                        .append("o valor pago por Pix não é reembolsado.\n\n");
            }

            if (!e.pelaBarbearia()) {
                int restantes = e.limite() - e.cancelamentosUsados();

                texto.append("SEUS CANCELAMENTOS NESTE MÊS\n")
                        .append("Você já cancelou ").append(e.cancelamentosUsados())
                        .append(" de ").append(e.limite()).append(" horários.\n");

                if (e.bloqueadoAte() == null && restantes == 1) {
                    texto.append("Atenção: falta apenas 1 cancelamento. Ao usá-lo, você ficará ")
                            .append(e.diasBloqueio()).append(" dias sem poder fazer novos agendamentos.\n");
                } else if (e.bloqueadoAte() == null && restantes > 1) {
                    texto.append("Você ainda pode cancelar mais ").append(restantes).append(" vezes neste mês.\n");
                }
                texto.append("\n");
            }

            if (e.bloqueadoAte() != null) {
                texto.append("SUA CONTA FOI BLOQUEADA PARA NOVOS AGENDAMENTOS\n")
                        .append("Você chegou ao limite de ").append(e.limite())
                        .append(" cancelamentos neste mês. Por isso, não será possível fazer novos agendamentos por ")
                        .append(e.diasBloqueio()).append(" dias, até ")
                        .append(e.bloqueadoAte().format(DATA)).append(".\n\n")
                        .append("Por que isso existe? Cada horário cancelado em cima da hora é um horário que ")
                        .append("outro cliente poderia ter usado. O limite mantém a agenda justa para todo mundo.\n\n")
                        .append("Aconteceu algum imprevisto, problema pessoal ou erro do sistema? ")
                        .append("Fale com a barbearia (pelos Chamados do app ou direto na loja). ")
                        .append("O administrador analisa o seu caso e, se for o caso, libera a sua conta.\n\n")
                        .append("Os horários que você já tem marcados continuam valendo. Como o limite de ")
                        .append("cancelamentos foi atingido, para cancelar um deles será preciso falar com a barbearia.\n\n");
            } else {
                texto.append("Você pode marcar um novo horário pelo BarberApp quando quiser.\n\n");
            }

            texto.append("Equipe BarberApp.");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(e.email());
            message.setSubject(e.bloqueadoAte() != null
                    ? "Agendamento cancelado e conta bloqueada - BarberApp"
                    : "Comprovante de cancelamento - BarberApp");
            message.setText(texto.toString());

            mailSender.send(message);
            log.info("Comprovante de cancelamento enviado para: {}", e.email());

        } catch (MailException ex) {
            log.error("Falha ao enviar comprovante de cancelamento para {}", e.email(), ex);
        }
    }

    @Async
    public void enviarDesbloqueio(ClienteDesbloqueadoEvent e) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(e.email());
            message.setSubject("Sua conta foi liberada - BarberApp");

            message.setText(
                    "Olá, " + e.clienteNome() + "!\n\n" +
                            "Boa notícia: a barbearia liberou a sua conta. Você já pode fazer novos agendamentos.\n\n" +
                            "O seu contador de cancelamentos foi zerado, então você tem " + e.limite() +
                            " cancelamentos disponíveis a partir de agora.\n\n" +
                            "Uma dica: se não puder comparecer, cancele com pelo menos 2 horas de antecedência. " +
                            "Assim o Pix que você pagou é reembolsado automaticamente e outro cliente pode usar o horário.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("Aviso de desbloqueio enviado para: {}", e.email());

        } catch (MailException ex) {
            log.error("Falha ao enviar aviso de desbloqueio para {}", e.email(), ex);
        }
    }

    @Async
    public void enviarVagaFila(VagaFilaEvent e) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(e.email());
            message.setSubject("Surgiu uma vaga! - BarberApp");

            message.setText(
                    "Olá, " + e.clienteNome() + "!\n\n" +
                            "Opa, surgiu uma vaga! Como você estava na fila de espera, " +
                            "já movemos o seu atendimento para o horário que abriu:\n\n" +
                            "Serviço: " + e.servicoNome() + "\n" +
                            "Barbeiro: " + e.barbeiroNome() + "\n" +
                            "Novo horário: " + e.novaData().format(DATA) + " às " + e.novoHorario().format(HORA) + "\n" +
                            "Horário anterior (liberado): " + e.dataAnterior().format(DATA) + " às " + e.horarioAnterior().format(HORA) + "\n\n" +
                            "Você deseja comparecer nesse novo horário? Se não puder, " +
                            "é só cancelar o agendamento pelo BarberApp.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("Aviso de vaga enviado para: {}", e.email());

        } catch (MailException ex) {
            log.error("Falha ao enviar aviso de vaga para {}", e.email(), ex);
        }
    }

    @Async
    public void enviarLembreteAgendamento(
            String destino,
            String clienteNome,
            String barbeiroNome,
            String servicoNome,
            LocalDate data,
            LocalTime horario
    ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(destino);
            message.setSubject("Lembrete de atendimento - BarberApp");

            message.setText(
                    "Olá, " + clienteNome + "!\n\n" +
                            "Seu atendimento está chegando!\n\n" +
                            "Serviço: " + servicoNome + "\n" +
                            "Barbeiro: " + barbeiroNome + "\n" +
                            "Data: " + data.format(DATA) + "\n" +
                            "Horário: " + horario.format(HORA) + "\n\n" +
                            "Falta aproximadamente uma hora para o atendimento.\n\n" +
                            "Não poderá comparecer? Acesse o BarberApp " +
                            "e cancele seu agendamento.\n\n" +
                            "Equipe BarberApp."
            );

            mailSender.send(message);
            log.info("Lembrete enviado para: {}", destino);

        } catch (MailException e) {
            log.error("Falha ao enviar lembrete para {}", destino, e);
        }
    }

    @Value("${barbearia.google.avaliacao-url}")
    private String googleAvaliacaoUrl;

    public void enviarConviteAvaliacao(String emailCliente, String nomeCliente) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setTo(emailCliente);
            helper.setSubject("Como foi sua experiência na nossa barbearia?");

            String html = """
            <div style="font-family:Arial,sans-serif;
                        max-width:600px;margin:auto;
                        padding:24px;color:#222;">

                <h2>Obrigado pela visita!</h2>

                <p>Olá, %s!</p>

                <p>Esperamos que tenha gostado do atendimento
                na nossa barbearia.</p>

                <p>Sua opinião é muito importante para nós.
                Que tal compartilhar sua experiência no Google?</p>

                <p style="margin:30px 0;">
                    <a href="%s"
                       style="background:#222;color:white;
                              padding:14px 22px;
                              text-decoration:none;
                              border-radius:6px;">
                        Avaliar no Google
                    </a>
                </p>

                <p>Obrigado pela preferência!</p>
            </div>
            """.formatted(
                    org.springframework.web.util.HtmlUtils.htmlEscape(nomeCliente),
                    org.springframework.web.util.HtmlUtils.htmlEscape(googleAvaliacaoUrl)
            );

            helper.setText(html, true);
            mailSender.send(mensagem);

        } catch (MessagingException e) {
            throw new IllegalStateException(
                    "Erro ao preparar o e-mail de avaliação.", e
            );
        }
    }
}
