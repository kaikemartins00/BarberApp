package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.*;
import com.kaikeMartins.barberapp_backend.dto.PagamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoConfirmadoDto;
import com.kaikeMartins.barberapp_backend.event.PagamentoAprovadoEvent;
import com.kaikeMartins.barberapp_backend.event.ReembolsoEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.kaikeMartins.barberapp_backend.exception.*;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private static final ZoneId FUSO =
            ZoneId.of("America/Sao_Paulo");

    private final PagamentoRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final MercadoPagoClient mercadoPago;

    private PagamentoResponse toResponse(PagamentoEntity p) {
        return new PagamentoResponse(
                p.getId(),
                p.getAgendamento().getId(),
                p.getValor(),
                p.getMetodo().name(),
                p.getStatus().name(),
                p.getConfirmadoEm()
        );
    }

    @Transactional
    public void criarPagamento(AgendamentoEntity agendamento) {
        if (repository.existsByAgendamentoId(agendamento.getId())) {
            throw new BadRequestException("Esse agendamento já possui pagamento.");
        }

        BigDecimal valor = BigDecimal.valueOf(agendamento.getServico().getPreco()).setScale(2);

        if (valor.signum() <= 0) {
            throw new BadRequestException("O valor do serviço deve ser maior que zero.");
        }

        PagamentoEntity pagamento = PagamentoEntity.builder()
                .agendamento(agendamento)
                .valor(valor)
                .metodo(PagamentoMetodo.PRESENCIAL)
                .status(PagamentoStatus.PENDENTE)
                .build();

        repository.save(pagamento);
    }

    @Transactional(readOnly = true)
    public PagamentoResponse consultar(Long agendamentoId) {

        PagamentoEntity pagamento = buscarPorAgendamento(agendamentoId);

        verificarAcesso(pagamento);

        return toResponse(pagamento);
    }

    @Transactional
    public PagamentoResponse escolherMetodo(Long agendamentoId, PagamentoMetodo metodo) {
        PagamentoEntity pagamento = bloquearPagamento(agendamentoId);

        UsuarioEntity usuario = usuarioAutenticado();

        if (!pagamento.getAgendamento().getCliente().getId().equals(usuario.getId())) {
            throw new ForbiddenException("Somente o cliente pode escolher a forma de pagamento.");
        }

        if (metodo == null) {
            throw new BadRequestException("Informe a forma de pagamento.");
        }

        validarAlteracaoCliente(pagamento);

        pagamento.setMetodo(metodo);
        pagamento.setStatus(PagamentoStatus.PENDENTE);

        return toResponse(repository.save(pagamento));
    }

    @Transactional
    public void confirmarPixValidado(String mercadoPagoId, java.math.BigDecimal valor, String referencia) {
        PagamentoEntity pagamento = repository.findByMercadoPagoId(mercadoPagoId)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));
        if (!String.valueOf(pagamento.getId()).equals(referencia)
                || pagamento.getValor().compareTo(valor) != 0
                || pagamento.getMetodo() != PagamentoMetodo.PIX
                || pagamento.getStatus() == PagamentoStatus.CANCELADO
                || pagamento.getAgendamento().getStatus() == AgendamentoStatus.CANCELADO) {
            throw new BadRequestException("Cobrança Pix não corresponde ao pagamento registrado.");
        }
        if (pagamento.getStatus() == PagamentoStatus.PAGO) return;
        pagamento.setStatus(PagamentoStatus.PAGO);
        pagamento.setConfirmadoEm(LocalDateTime.now(FUSO));
        pagamento.getAgendamento().setStatus(AgendamentoStatus.CONFIRMADO);
        repository.save(pagamento);
        publicarConfirmacao(pagamento);

        AgendamentoEntity ag = pagamento.getAgendamento();
        eventPublisher.publishEvent(new PagamentoAprovadoEvent(
                ag.getId(),
                ag.getCliente().getUsuario().getEmail(),
                ag.getCliente().getNome(),
                ag.getBarbeiro().getNome(),
                ag.getServico().getNome(),
                ag.getData(),
                ag.getHorario(),
                pagamento.getValor()
        ));
    }

    private boolean elegivelParaReembolso(PagamentoEntity pagamento) {
        return pagamento != null
                && pagamento.getStatus() == PagamentoStatus.PAGO
                && pagamento.getMetodo() == PagamentoMetodo.PIX
                && pagamento.getMercadoPagoId() != null;
    }

    @Transactional(readOnly = true)
    public boolean podeReembolsar(AgendamentoEntity ag) {
        return elegivelParaReembolso(repository.findByAgendamentoId(ag.getId()).orElse(null));
    }

    private void efetuarReembolso(PagamentoEntity pagamento) {
        mercadoPago.reembolsar(pagamento.getMercadoPagoId());

        pagamento.setStatus(PagamentoStatus.CANCELADO);
        pagamento.setReembolsadoEm(LocalDateTime.now(FUSO));
        pagamento.setReembolsoPendente(false);
        repository.save(pagamento);

        AgendamentoEntity ag = pagamento.getAgendamento();

        eventPublisher.publishEvent(new ReembolsoEvent(
                ag.getId(),
                ag.getCliente().getUsuario().getEmail(),
                ag.getCliente().getNome(),
                ag.getServico().getNome(),
                ag.getData(),
                ag.getHorario(),
                pagamento.getValor(),
                pagamento.getMercadoPagoId(),
                pagamento.getReembolsadoEm()
        ));
    }

    @Transactional
    public boolean reembolsarPorFalta(AgendamentoEntity ag) {
        PagamentoEntity pagamento = repository.findByAgendamentoId(ag.getId()).orElse(null);

        if (!elegivelParaReembolso(pagamento)) {
            return false;
        }

        try {
            efetuarReembolso(pagamento);
        } catch (IllegalStateException e) {
            throw new BadRequestException(
                    "Não foi possível reembolsar o Pix agora. Tente novamente em instantes.");
        }

        return true;
    }

    @Transactional
    public void reembolsarPorCancelamento(AgendamentoEntity ag) {
        PagamentoEntity pagamento = repository.findByAgendamentoId(ag.getId()).orElse(null);

        if (!elegivelParaReembolso(pagamento)) {
            return;
        }

        try {
            efetuarReembolso(pagamento);
        } catch (IllegalStateException e) {
            log.warn("Reembolso do agendamento {} ficou pendente e será tentado de novo: {}",
                    ag.getId(), e.getMessage());
            pagamento.setReembolsoPendente(true);
            repository.save(pagamento);
        }
    }

    @Transactional
    public void reprocessarReembolsosPendentes() {
        for (PagamentoEntity pagamento : repository.findByReembolsoPendenteTrue()) {
            if (!elegivelParaReembolso(pagamento)) {
                pagamento.setReembolsoPendente(false);
                repository.save(pagamento);
                continue;
            }

            try {
                efetuarReembolso(pagamento);
            } catch (IllegalStateException e) {
                log.warn("Reembolso pendente do pagamento {} ainda não foi possível: {}",
                        pagamento.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void confirmarReservaPresencial(Long agendamentoId) {
        PagamentoEntity pagamento = bloquearPagamento(agendamentoId);
        if (pagamento.getMetodo() != PagamentoMetodo.PRESENCIAL || pagamento.getStatus() != PagamentoStatus.PENDENTE) {
            throw new BadRequestException("Reserva presencial inválida.");
        }
        if (pagamento.getAgendamento().getStatus() != AgendamentoStatus.PENDENTE) {
            throw new BadRequestException("Agendamento não está aguardando confirmação.");
        }
        pagamento.getAgendamento().setStatus(AgendamentoStatus.CONFIRMADO);
        publicarConfirmacao(pagamento);
    }

    private void publicarConfirmacao(PagamentoEntity pagamento) {
        AgendamentoEntity ag = pagamento.getAgendamento();
        eventPublisher.publishEvent(new AgendamentoConfirmadoDto(
                ag.getCliente().getUsuario().getEmail(),
                ag.getCliente().getNome(),
                ag.getBarbeiro().getNome(),
                ag.getServico().getNome(),
                ag.getData(),
                ag.getHorario()
        ));
    }

    @Transactional
    public PagamentoResponse informarPix(Long agendamentoId) {
        throw new BadRequestException("O Pix é confirmado automaticamente pelo Mercado Pago.");
    }

    @Transactional
    public PagamentoResponse confirmar(Long agendamentoId) {

        PagamentoEntity pagamento = bloquearPagamento(agendamentoId);

        UsuarioEntity usuario = usuarioAutenticado();

        boolean admin = temRole(usuario, "ROLE_ADMIN");

        boolean barbeiroResponsavel =
                temRole(usuario, "ROLE_BARBEIRO")
                        && pagamento.getAgendamento()
                        .getBarbeiro().getId()
                        .equals(usuario.getId());

        if (!admin && !barbeiroResponsavel) {
            throw new ForbiddenException("Você não pode confirmar esse pagamento.");
        }

        if (pagamento.getStatus() == PagamentoStatus.PAGO) {
            throw new BadRequestException("Esse pagamento já foi confirmado.");
        }

        if (pagamento.getStatus() == PagamentoStatus.CANCELADO || pagamento.getAgendamento().getStatus() == AgendamentoStatus.CANCELADO) {
            throw new BadRequestException("Não é possível confirmar um pagamento cancelado.");
        }

        if (pagamento.getMetodo() == PagamentoMetodo.PIX) {
            throw new BadRequestException("O Pix é confirmado automaticamente pelo provedor.");
        }


        pagamento.setStatus(PagamentoStatus.PAGO);
        pagamento.setConfirmadoEm(LocalDateTime.now(FUSO));
        pagamento.setConfirmadoPor(usuario);

        return toResponse(repository.save(pagamento));
    }

    private void validarAlteracaoCliente(
            PagamentoEntity pagamento
    ) {
        if (pagamento.getStatus() != PagamentoStatus.PENDENTE) {
            throw new BadRequestException("Não é possível alterar esse pagamento.");
        }

        if (pagamento.getAgendamento().getStatus() == AgendamentoStatus.CANCELADO) {
            throw new BadRequestException("Esse agendamento foi cancelado.");
        }

        LocalDateTime atendimento = LocalDateTime.of(
                pagamento.getAgendamento().getData(),
                pagamento.getAgendamento().getHorario()
        );

        if (!atendimento.isAfter(LocalDateTime.now(FUSO))) {
            throw new BadRequestException("O prazo para alterar o pagamento terminou.");
        }
    }

    private PagamentoEntity bloquearPagamento(Long id) {
        return repository.buscarParaAtualizacao(id)
                .orElseThrow(() -> new NotFoundException(
                        "Pagamento não encontrado."
                ));
    }

    private PagamentoEntity buscarPorAgendamento(Long id) {
        return repository.findByAgendamentoId(id)
                .orElseThrow(() -> new NotFoundException(
                        "Pagamento não encontrado."
                ));
    }

    private void verificarAcesso(PagamentoEntity pagamento) {

        UsuarioEntity usuario = usuarioAutenticado();

        boolean cliente = pagamento.getAgendamento()
                .getCliente().getId().equals(usuario.getId());

        boolean barbeiro = temRole(usuario, "ROLE_BARBEIRO")
                && pagamento.getAgendamento()
                .getBarbeiro().getId().equals(usuario.getId());

        if (!cliente && !barbeiro
                && !temRole(usuario, "ROLE_ADMIN")) {
            throw new ForbiddenException(
                    "Você não tem acesso a esse pagamento."
            );
        }
    }

    private UsuarioEntity usuarioAutenticado() {

        var authentication = SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal()
                instanceof UsuarioEntity usuario) {
            return usuario;
        }

        throw new ForbiddenException(
                "Usuário não autenticado."
        );
    }

    private boolean temRole(UsuarioEntity usuario, String role) {
        return usuario.getRoles().stream()
                .anyMatch(r -> role.equals(r.getAuthority()));
    }
}