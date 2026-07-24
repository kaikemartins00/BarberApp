package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;

    public AgendamentoResponse create(AgendamentoRequest request) {
        ClienteEntity cliente = clienteAutenticado();

        BarbeiroEntity barbeiro = barbeiroRepository.findById(request.barbeiroId())
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));

        ServicoEntity servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado"));

        if (!barbeiro.getServicos().contains(servico)) {
            throw new BadRequestException("Esse barbeiro não realiza esse serviço.");
        }

        if (!barbeiro.getHorariosTrabalho().contains(request.horario())) {
            throw new BadRequestException("Esse horário não está entre os horários de trabalho do barbeiro.");
        }

        boolean conflito = repository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                barbeiro.getId(), request.data(), request.horario(), "cancelado"
        );

        if (conflito) {
            throw new BadRequestException("Esse horário já está ocupado.");
        }

        AgendamentoEntity agendamento = AgendamentoEntity.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(request.data())
                .horario(request.horario())
                .status("pendente")
                .build();

        return toResponse(repository.save(agendamento));
    }

    public List<AgendamentoResponse> meusAgendamentos() {
        return repository.findByClienteId(clienteAutenticado().getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AgendamentoResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AgendamentoResponse atualizarStatus(Long id, String novoStatus) {
        AgendamentoEntity agendamento = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado"));
        agendamento.setStatus(novoStatus);
        return toResponse(repository.save(agendamento));
    }

    private ClienteEntity clienteAutenticado() {
        return (ClienteEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private AgendamentoResponse toResponse(AgendamentoEntity a) {
        return new AgendamentoResponse(
                a.getId(),
                a.getCliente().getNome(),
                a.getBarbeiro().getNome(),
                a.getServico().getNome(),
                a.getServico().getPreco(),
                a.getData(),
                a.getHorario(),
                a.getStatus()
        );
    }
}
