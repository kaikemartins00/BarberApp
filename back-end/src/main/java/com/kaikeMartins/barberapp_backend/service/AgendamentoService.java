package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.ResumoCancelamentosResponse;
import com.kaikeMartins.barberapp_backend.event.ComprovanteCancelamentoEvent;
import com.kaikeMartins.barberapp_backend.event.AgendamentoCriadoEvent;
import com.kaikeMartins.barberapp_backend.event.AvaliacaoSolicitadaEvent;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    static final int LIMITE_CANCELAMENTOS_MES = 3;
    private static final int DIAS_BLOQUEIO = 7;
    private static final long HORAS_ANTECEDENCIA_REEMBOLSO = 2;

    private static final Set<AgendamentoStatus> ATIVOS = Set.of(AgendamentoStatus.PENDENTE, AgendamentoStatus.CONFIRMADO);

    private final AgendamentoRepository repository;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final ClienteRepository clienteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PagamentoService pagamentoService;
    private final PagamentoRepository pagamentoRepository;
    private final AtendimentoServicoService atendimentoServicoService;
    private final FilaEsperaService filaEsperaService;

    private AgendamentoResponse toResponse(AgendamentoEntity e) {
        return new AgendamentoResponse(
                e.getId(),
                e.getCliente().getNome(),
                e.getBarbeiro().getNome(),
                e.getServico().getNome(),
                e.getServico().getPreco(),
                e.getData(),
                e.getHorario(),
                e.getStatus().name()
        );
    }

    @Transactional
    public AgendamentoResponse create(AgendamentoRequest request) {

        ClienteEntity cliente = clienteAutenticado();

        LocalDateTime agora = LocalDateTime.now(FUSO);

        if (cliente.getBloqueadoAte() != null && cliente.getBloqueadoAte().isAfter(agora)) {
            throw new BadRequestException("Você não pode agendar até "
                    + cliente.getBloqueadoAte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " por excesso de cancelamentos neste mês. Entre em contato com a barbearia.");
        }

        LocalDateTime dataHorario =
                LocalDateTime.of(request.data(), request.horario());

        if (!dataHorario.isAfter(agora)) {
            throw new BadRequestException("Não é possível agendar um horário que já passou.");
        }

        BarbeiroEntity barbeiro = barbeiroRepository.findById(request.barbeiroId())
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado.")
                );

        ServicoEntity servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() ->
                        new NotFoundException("Serviço não encontrado.")
                );

        boolean fazOServico = barbeiro.getServicos().stream().anyMatch(s -> s.getId().equals(servico.getId()));

        if (!fazOServico) {
            throw new BadRequestException("Esse barbeiro não realiza esse serviço."
            );
        }

        if (!barbeiro.getHorariosTrabalho().contains(dataHorario)) {
            throw new BadRequestException("O barbeiro não atende nesse dia e horário.");
        }

        if (repository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(barbeiro.getId(), request.data(), request.horario(), AgendamentoStatus.CANCELADO)) {
            throw new BadRequestException("Esse horário já está ocupado.");
        }

        AgendamentoEntity agendamento = AgendamentoEntity.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(request.data())
                .horario(request.horario())
                .status(AgendamentoStatus.PENDENTE)
                .build();

        AgendamentoEntity salvo = repository.save(agendamento);

        pagamentoService.criarPagamento(salvo);

        atendimentoServicoService.registrarServicoInicial(salvo);

        eventPublisher.publishEvent(new AgendamentoCriadoEvent(
                salvo.getId(),
                cliente.getUsuario().getEmail(),
                cliente.getNome(),
                barbeiro.getNome(),
                servico.getNome(),
                servico.getPreco(),
                salvo.getData(),
                salvo.getHorario()
        ));

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<LocalTime> horariosLivres(Long barbeiroId, LocalDate data) {
        BarbeiroEntity barbeiro = barbeiroRepository.findById(barbeiroId).orElseThrow(() -> new NotFoundException("Barbeiro não encontrado."));

        Set<LocalTime> ocupados = repository.findByBarbeiroIdAndData(barbeiroId, data)
                .stream()
                .filter(a -> a.getStatus() != AgendamentoStatus.CANCELADO)
                .map(AgendamentoEntity::getHorario)
                .collect(Collectors.toSet());

        LocalDateTime agora = LocalDateTime.now(FUSO);

        return barbeiro.getHorariosTrabalho().stream()
                .filter(dh -> dh.toLocalDate().equals(data))
                .map(LocalDateTime::toLocalTime)
                .filter(h -> !ocupados.contains(h))
                .filter(h -> LocalDateTime.of(data, h).isAfter(agora))
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> meusAgendamentos() {
        return repository.findByClienteId(clienteAutenticado().getId())
                .stream()
                .filter(ag -> ag.getStatus() != AgendamentoStatus.PENDENTE ||
                        pagamentoRepository.findByAgendamentoId(ag.getId())
                                .map(p -> p.getMetodo() != PagamentoMetodo.PIX || p.getStatus() == PagamentoStatus.PAGO)
                                .orElse(false))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> findAll() {

        UsuarioEntity usuario = usuarioAutenticado();

        List<AgendamentoEntity> lista =
                temRole(usuario, "ROLE_BARBEIRO")
                        ? repository.findByBarbeiroId(usuario.getId())
                        : repository.findAll();

        return lista.stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public AgendamentoResponse registrarComparecimento(Long id, AgendamentoStatus novoStatus) {

        if (novoStatus != AgendamentoStatus.CONCLUIDO && novoStatus != AgendamentoStatus.NAO_COMPARECEU) {
            throw new BadRequestException("Status de comparecimento inválido.");
        }

        UsuarioEntity usuario = usuarioAutenticado();

        if (!temRole(usuario, "ROLE_BARBEIRO")
                && !temRole(usuario, "ROLE_ADMIN")) {
            throw new ForbiddenException("Apenas barbeiros e administradores podem registrar comparecimento.");
        }

        AgendamentoEntity ag = repository.findById(id).orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (temRole(usuario, "ROLE_BARBEIRO") && !temRole(usuario, "ROLE_ADMIN") && !ag.getBarbeiro().getId().equals(usuario.getId())) {
            throw new ForbiddenException("Esse agendamento pertence a outro barbeiro.");
        }

        if (!ATIVOS.contains(ag.getStatus())) {
            throw new BadRequestException("Esse atendimento já foi finalizado ou cancelado.");
        }

        LocalDateTime inicio = LocalDateTime.of(ag.getData(), ag.getHorario());

        if (inicio.isAfter(LocalDateTime.now(FUSO))) {
            throw new BadRequestException("Aguarde o início do horário para registrar o comparecimento.");
        }

        ag.setStatus(novoStatus);

        AgendamentoEntity salvo = repository.save(ag);

        if (novoStatus == AgendamentoStatus.NAO_COMPARECEU) {
            pagamentoService.reembolsarPorFalta(salvo);
        }

        if (novoStatus == AgendamentoStatus.CONCLUIDO) {
            eventPublisher.publishEvent(
                    new AvaliacaoSolicitadaEvent(
                            salvo.getId(),
                            salvo.getCliente().getUsuario().getEmail(),
                            salvo.getCliente().getNome()
                    )
            );
        }
        return toResponse(salvo);
    }


    @Transactional
    public AgendamentoResponse atualizarStatus(Long id, AgendamentoStatus novoStatus) {
        AgendamentoEntity ag = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));

        if (!ATIVOS.contains(ag.getStatus())) {
            throw new BadRequestException("Esse agendamento já foi finalizado ou cancelado.");
        }

        UsuarioEntity usuario = usuarioAutenticado();

        if (temRole(usuario, "ROLE_BARBEIRO")) {
            if (!ag.getBarbeiro().getId().equals(usuario.getId())) {
                throw new ForbiddenException("Esse agendamento é de outro barbeiro.");
            }
        } else if (!temRole(usuario, "ROLE_ADMIN")) {
            if (!ag.getCliente().getId().equals(usuario.getId())) {
                throw new ForbiddenException("Esse agendamento não é seu.");
            }
            if (novoStatus != AgendamentoStatus.CANCELADO) {
                throw new ForbiddenException("Clientes só podem cancelar agendamentos.");
            }
        }

        if (novoStatus != AgendamentoStatus.CANCELADO && novoStatus != AgendamentoStatus.CONFIRMADO) {
            throw new BadRequestException("Use o registro de comparecimento para concluir ou registrar falta.");
        }

        if (novoStatus == AgendamentoStatus.CONFIRMADO &&
                !temRole(usuario, "ROLE_ADMIN") &&
                !temRole(usuario, "ROLE_BARBEIRO")) {
            throw new ForbiddenException("Clientes não podem confirmar atendimentos.");
        }

        LocalDateTime agora = LocalDateTime.now(FUSO);
        boolean cancelaCliente = novoStatus == AgendamentoStatus.CANCELADO
                && !temRole(usuario, "ROLE_BARBEIRO")
                && !temRole(usuario, "ROLE_ADMIN");

        if (cancelaCliente
                && cancelamentosNoMes(ag.getCliente(), agora) >= LIMITE_CANCELAMENTOS_MES) {
            throw new BadRequestException("Você já usou os " + LIMITE_CANCELAMENTOS_MES
                    + " cancelamentos permitidos neste mês. Entre em contato com a barbearia.");
        }

        ag.setStatus(novoStatus);

        if (novoStatus == AgendamentoStatus.CANCELADO) {
            ag.setCanceladoEm(agora);
            ag.setCanceladoPeloCliente(cancelaCliente);
        }

        AgendamentoEntity salvo = repository.saveAndFlush(ag);

        if (novoStatus == AgendamentoStatus.CANCELADO) {
            filaEsperaService.processarCancelamento(salvo);

            boolean comAntecedencia = !LocalDateTime.of(salvo.getData(), salvo.getHorario())
                    .isBefore(agora.plusHours(HORAS_ANTECEDENCIA_REEMBOLSO));

            if (cancelaCliente) {
                aplicarBloqueioSeNecessario(salvo.getCliente(), agora);

                if (comAntecedencia) {
                    pagamentoService.reembolsarPorCancelamento(salvo);
                }
            } else {
                pagamentoService.reembolsarPorCancelamento(salvo);
            }

            long usados = cancelamentosNoMes(salvo.getCliente(), agora);

            eventPublisher.publishEvent(new ComprovanteCancelamentoEvent(
                    salvo.getId(),
                    salvo.getCliente().getUsuario().getEmail(),
                    salvo.getCliente().getNome(),
                    salvo.getServico().getNome(),
                    salvo.getBarbeiro().getNome(),
                    salvo.getData(),
                    salvo.getHorario(),
                    !cancelaCliente,
                    comAntecedencia,
                    (int) usados,
                    LIMITE_CANCELAMENTOS_MES,
                    cancelaCliente && usados >= LIMITE_CANCELAMENTOS_MES ? salvo.getCliente().getBloqueadoAte() : null,
                    DIAS_BLOQUEIO
            ));
        }

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public ResumoCancelamentosResponse resumoCancelamentos() {
        ClienteEntity cliente = clienteAutenticado();
        LocalDateTime agora = LocalDateTime.now(FUSO);

        LocalDateTime bloqueadoAte = cliente.getBloqueadoAte() != null
                && cliente.getBloqueadoAte().isAfter(agora)
                ? cliente.getBloqueadoAte()
                : null;

        return new ResumoCancelamentosResponse(
                (int) cancelamentosNoMes(cliente, agora),
                LIMITE_CANCELAMENTOS_MES,
                DIAS_BLOQUEIO,
                bloqueadoAte
        );
    }

    private long cancelamentosNoMes(ClienteEntity cliente, LocalDateTime agora) {
        LocalDate hoje = agora.toLocalDate();

        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime inicio = cliente.getDesbloqueadoEm() != null && cliente.getDesbloqueadoEm().isAfter(inicioMes)
                ? cliente.getDesbloqueadoEm()
                : inicioMes;

        return repository.countByClienteIdAndCanceladoPeloClienteTrueAndCanceladoEmBetween(
                cliente.getId(),
                inicio,
                hoje.withDayOfMonth(hoje.lengthOfMonth()).atTime(LocalTime.MAX)
        );
    }

    private void aplicarBloqueioSeNecessario(ClienteEntity cliente, LocalDateTime agora) {
        if (cancelamentosNoMes(cliente, agora) >= LIMITE_CANCELAMENTOS_MES) {
            cliente.setBloqueadoAte(agora.plusDays(DIAS_BLOQUEIO));
            clienteRepository.save(cliente);
        }
    }

    private UsuarioEntity usuarioAutenticado() {

        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof UsuarioEntity usuario) {
            return usuario;
        }

        throw new ForbiddenException("Usuário não autenticado ou sessão inválida.");
    }

    private ClienteEntity clienteAutenticado() {
        return clienteRepository.findById(usuarioAutenticado().getId()).orElseThrow(() -> new ForbiddenException("Apenas clientes podem usar esta função."));
    }

    private boolean temRole(UsuarioEntity usuario, String role) {
        return usuario.getRoles().stream().anyMatch(r -> role.equals(r.getAuthority()));
    }
}