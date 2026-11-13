package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AdicionarServicoAtendimentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AlterarValorServicoRequest;
import com.kaikeMartins.barberapp_backend.dto.ContaAtendimentoResponse;
import com.kaikeMartins.barberapp_backend.dto.ServicoAtendimentoResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtendimentoServicoService {

    private final AgendamentoServicoRepository itemRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final ServicoRepository servicoRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registrarServicoInicial(AgendamentoEntity agendamento) {
        if (!itemRepository.findByAgendamentoIdOrderByIdAsc(agendamento.getId()).isEmpty()) {
            throw new BadRequestException("Os serviços deste atendimento já foram registrados.");
        }

        BigDecimal valor = normalizarValor(BigDecimal.valueOf(agendamento.getServico().getPreco()));

        AgendamentoServicoEntity item = AgendamentoServicoEntity.builder()
                        .agendamento(agendamento)
                        .servico(agendamento.getServico())
                        .valorCobrado(valor)
                        .extra(false)
                        .build();

        itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public ContaAtendimentoResponse consultar(Long agendamentoId) {

        AgendamentoEntity agendamento = buscarAgendamento(agendamentoId);

        verificarAcesso(agendamento);

        PagamentoEntity pagamento = pagamentoRepository
                .findByAgendamentoId(agendamentoId)
                .orElseThrow(() -> new NotFoundException(
                        "Pagamento não encontrado."
                ));

        return montarResposta(agendamentoId, pagamento);
    }

    @Transactional
    public ContaAtendimentoResponse adicionar(Long agendamentoId, AdicionarServicoAtendimentoRequest request) {
        PagamentoEntity pagamento = bloquearPagamento(agendamentoId);

        validarEdicao(pagamento);

        ServicoEntity servico = servicoRepository
                .findById(request.servicoId())
                .orElseThrow(() -> new NotFoundException(
                        "Serviço não encontrado."
                ));

        AgendamentoEntity agendamento = pagamento.getAgendamento();

        boolean fazOServico = agendamento.getBarbeiro()
                .getServicos()
                .stream()
                .anyMatch(s -> s.getId().equals(servico.getId()));

        if (!fazOServico) {
            throw new BadRequestException("Esse barbeiro não realiza esse serviço.");
        }

        BigDecimal valor = request.valorCobrado() != null
                ? request.valorCobrado()
                : BigDecimal.valueOf(servico.getPreco());

        AgendamentoServicoEntity item = AgendamentoServicoEntity.builder()
                        .agendamento(agendamento)
                        .servico(servico)
                        .valorCobrado(normalizarValor(valor))
                        .extra(true)
                        .build();

        itemRepository.saveAndFlush(item);

        atualizarTotal(pagamento);

        return montarResposta(agendamentoId, pagamento);
    }

    @Transactional
    public ContaAtendimentoResponse alterarValor(Long agendamentoId, Long itemId, AlterarValorServicoRequest request) {
        PagamentoEntity pagamento = bloquearPagamento(agendamentoId);

        validarEdicao(pagamento);

        AgendamentoServicoEntity item = buscarItem(itemId, agendamentoId);

        item.setValorCobrado(normalizarValor(request.valorCobrado()));

        itemRepository.saveAndFlush(item);

        atualizarTotal(pagamento);

        return montarResposta(agendamentoId, pagamento);
    }

    @Transactional
    public ContaAtendimentoResponse removerExtra(Long agendamentoId, Long itemId) {
        PagamentoEntity pagamento = bloquearPagamento(agendamentoId);

        validarEdicao(pagamento);

        AgendamentoServicoEntity item = buscarItem(itemId, agendamentoId);

        if (!item.isExtra()) {
            throw new BadRequestException("O serviço original do agendamento não pode ser removido.");}

        itemRepository.delete(item);
        itemRepository.flush();

        atualizarTotal(pagamento);

        return montarResposta(agendamentoId, pagamento);
    }

    private void atualizarTotal(PagamentoEntity pagamento) {

        BigDecimal total = itemRepository
                .findByAgendamentoIdOrderByIdAsc(
                        pagamento.getAgendamento().getId()
                )
                .stream()
                .map(AgendamentoServicoEntity::getValorCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.signum() <= 0) {
            throw new BadRequestException("O total do atendimento deve ser maior que zero.");
        }

        pagamento.setValor(total.setScale(
                2, RoundingMode.UNNECESSARY));

        if (pagamento.getStatus() == PagamentoStatus.AGUARDANDO_CONFIRMACAO) {
            pagamento.setStatus(PagamentoStatus.PENDENTE);
        }

        pagamentoRepository.save(pagamento);
    }

    private void validarEdicao(PagamentoEntity pagamento) {

        AgendamentoEntity agendamento = pagamento.getAgendamento();

        UsuarioEntity usuario = usuarioAutenticado();

        boolean admin = temRole(usuario, "ROLE_ADMIN");

        boolean barbeiro = temRole(usuario, "ROLE_BARBEIRO") && agendamento.getBarbeiro().getId().equals(usuario.getId());

        if (!admin && !barbeiro) {
            throw new ForbiddenException("Somente o barbeiro responsável ou administrador pode alterar a conta.");
        }

        if (pagamento.getStatus() == PagamentoStatus.PAGO || pagamento.getStatus() == PagamentoStatus.CANCELADO) {
            throw new BadRequestException("Esse pagamento não permite alterações.");
        }

        if (agendamento.getStatus() == AgendamentoStatus.CANCELADO || agendamento.getStatus() == AgendamentoStatus.NAO_COMPARECEU) {
            throw new BadRequestException("Não é possível alterar a conta desse atendimento.");
        }
    }

    private PagamentoEntity bloquearPagamento(Long agendamentoId) {
        return pagamentoRepository
                .buscarParaAtualizacao(agendamentoId)
                .orElseThrow(() -> new NotFoundException(
                        "Pagamento não encontrado."
                ));
    }

    private AgendamentoEntity buscarAgendamento(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Agendamento não encontrado."
                ));
    }

    private AgendamentoServicoEntity buscarItem(Long itemId, Long agendamentoId) {
        return itemRepository
                .findByIdAndAgendamentoId(itemId, agendamentoId)
                .orElseThrow(() -> new NotFoundException(
                        "Serviço do atendimento não encontrado."
                ));
    }

    private ContaAtendimentoResponse montarResposta(Long agendamentoId, PagamentoEntity pagamento) {
        List<ServicoAtendimentoResponse> itens =
                itemRepository
                        .findByAgendamentoIdOrderByIdAsc(
                                agendamentoId
                        )
                        .stream()
                        .map(item -> new ServicoAtendimentoResponse(
                                item.getId(),
                                item.getServico().getId(),
                                item.getServico().getNome(),
                                item.getValorCobrado(),
                                item.isExtra()
                        ))
                        .toList();

        return new ContaAtendimentoResponse(
                agendamentoId,
                itens,
                pagamento.getValor(),
                pagamento.getStatus().name()
        );
    }

    private void verificarAcesso(AgendamentoEntity agendamento) {
        UsuarioEntity usuario = usuarioAutenticado();

        boolean cliente = agendamento.getCliente()
                .getId().equals(usuario.getId());

        boolean barbeiro = temRole(usuario, "ROLE_BARBEIRO")
                && agendamento.getBarbeiro().getId()
                .equals(usuario.getId());

        if (!cliente && !barbeiro
                && !temRole(usuario, "ROLE_ADMIN")) {
            throw new ForbiddenException(
                    "Você não tem acesso a esse atendimento."
            );
        }
    }

    private BigDecimal normalizarValor(BigDecimal valor) {

        if (valor == null || valor.signum() <= 0
                || valor.scale() > 2
                || valor.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new BadRequestException(
                    "Informe um valor válido, maior que zero e com até duas casas decimais."
            );
        }

        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    private UsuarioEntity usuarioAutenticado() {
        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof UsuarioEntity usuario) {
            return usuario;
        }

        throw new ForbiddenException("Usuário não autenticado.");
    }

    private boolean temRole(UsuarioEntity usuario, String role) {
        return usuario.getRoles().stream()
                .anyMatch(r -> role.equals(r.getAuthority()));
    }
}
