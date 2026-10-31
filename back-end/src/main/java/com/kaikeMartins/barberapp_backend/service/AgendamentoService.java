package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final ClienteRepository clienteRepository;

    private AgendamentoResponse toResponse(AgendamentoEntity e) {
        return new AgendamentoResponse(
                e.getId(),
                e.getCliente().getNome(),
                e.getBarbeiro().getNome(),
                e.getServico().getNome(),
                e.getServico().getPreco(),
                e.getData(),
                e.getHorario(),
                e.getStatus().name());
    }

    @Transactional
    public AgendamentoResponse create(AgendamentoRequest request) {
        ClienteEntity cliente = clienteAutenticado();

        BarbeiroEntity barbeiro = barbeiroRepository.findById(request.barbeiroId())
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));

        ServicoEntity servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado"));

        LocalDateTime dataHorarioAgendamento = LocalDateTime.of(request.data(), request.horario());

        boolean fazOServico = barbeiro.getServicos().stream()
                .anyMatch(s -> s.getId().equals(servico.getId()));

        if (!fazOServico) {
            throw new BadRequestException("Esse barbeiro não realiza esse serviço.");
        }

        if (!barbeiro.getHorariosTrabalho().contains(dataHorarioAgendamento)) {
            throw new BadRequestException("O barbeiro não atende nesse dia e horário.");
        }

        if (dataHorarioAgendamento.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Esse horário já passou.");
        }

        if (repository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                barbeiro.getId(), request.data(), request.horario(), AgendamentoStatus.CANCELADO)) {
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

        return toResponse(repository.save(agendamento));
    }

    @Transactional(readOnly = true)
    public List<LocalTime> horariosLivres(Long barbeiroId, LocalDate data) {
        BarbeiroEntity barbeiro = barbeiroRepository.findById(barbeiroId)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));

        Set<LocalTime> ocupados = repository.findByBarbeiroIdAndData(barbeiroId, data).stream()
                .filter(a -> a.getStatus() != AgendamentoStatus.CANCELADO)
                .map(AgendamentoEntity::getHorario)
                .collect(Collectors.toSet());

        LocalDateTime agora = LocalDateTime.now();

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
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> findAll() {
        UsuarioEntity usuario = usuarioAutenticado();

        List<AgendamentoEntity> lista = temRole(usuario, "ROLE_BARBEIRO")
                ? repository.findByBarbeiroId(usuario.getId())
                : repository.findAll();

        return lista.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AgendamentoResponse atualizarStatus(Long id, AgendamentoStatus novoStatus) {
        AgendamentoEntity ag = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado"));

        if (ag.getStatus() == AgendamentoStatus.CANCELADO) {
            throw new BadRequestException("Esse agendamento já foi cancelado.");
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

        ag.setStatus(novoStatus);
        return toResponse(repository.save(ag));
    }

    private UsuarioEntity usuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UsuarioEntity usuario) {
            return usuario;
        }
        throw new ForbiddenException("Usuário não autenticado ou sessão inválida.");
    }

    private ClienteEntity clienteAutenticado() {
        return clienteRepository.findById(usuarioAutenticado().getId())
                .orElseThrow(() -> new ForbiddenException("Apenas clientes podem usar esta função."));
    }

    private boolean temRole(UsuarioEntity usuario, String role) {
        return usuario.getRoles().stream().anyMatch(r -> role.equals(r.getAuthority()));
    }
}