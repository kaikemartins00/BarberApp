package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroResponse;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroUpdateRequest;
import com.kaikeMartins.barberapp_backend.dto.CancelamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.CancelarDiaRequest;
import com.kaikeMartins.barberapp_backend.dto.HorariosRequest;
import com.kaikeMartins.barberapp_backend.dto.ServicosIdsRequest;
import com.kaikeMartins.barberapp_backend.event.AgendamentosCanceladosEvent;
import com.kaikeMartins.barberapp_backend.event.AvisoCancelamento;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import com.kaikeMartins.barberapp_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final UserRepository userRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    private BarbeiroResponse toResponse(BarbeiroEntity e) {
        return new BarbeiroResponse(
                e.getId(),
                e.getNome(),
                e.getFotoUrl(),
                new HashSet<>(e.getServicos()),
                e.getHorariosTrabalho().stream().sorted().toList());
    }

    private BarbeiroEntity buscar(Long id) {
        return barbeiroRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<BarbeiroResponse> findAll() {
        return barbeiroRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BarbeiroResponse findById(Long id) {
        return toResponse(buscar(id));
    }

    public BarbeiroResponse update(Long id, BarbeiroUpdateRequest request) {
        BarbeiroEntity barbeiro = buscar(id);
        UsuarioEntity usuario = barbeiro.getUsuario();

        if (!usuario.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("O email já existe.");
        }

        usuario.setEmail(request.email());
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }

        userRepository.save(usuario);

        barbeiro.setFotoUrl(request.fotoUrl());

        return toResponse(barbeiroRepository.save(barbeiro));
    }

    public BarbeiroResponse atualizarHorarios(Long id, HorariosRequest request) {
        BarbeiroEntity barbeiro = buscar(id);
        barbeiro.setHorariosTrabalho(new HashSet<>(request.horarios()));
        return toResponse(barbeiroRepository.save(barbeiro));
    }

    public BarbeiroResponse atualizarServicos(Long id, ServicosIdsRequest request) {
        BarbeiroEntity barbeiro = buscar(id);
        barbeiro.setServicos(new HashSet<>(servicoRepository.findAllById(request.servicosIds())));
        return toResponse(barbeiroRepository.save(barbeiro));
    }

    public CancelamentoResponse cancelarDia(Long id, CancelarDiaRequest request) {
        BarbeiroEntity barbeiro = buscar(id);

        LocalDate inicio = request.data();
        LocalDate fim = request.ateData() != null ? request.ateData() : inicio;

        if (fim.isBefore(inicio)) {
            throw new BadRequestException("A data final não pode ser anterior à data inicial.");
        }

        LocalDateTime agora = LocalDateTime.now();

        List<AgendamentoEntity> afetados = agendamentoRepository
                .findByBarbeiroIdAndDataBetweenAndStatusIn(id, inicio, fim,
                        List.of(AgendamentoStatus.PENDENTE, AgendamentoStatus.CONFIRMADO))
                .stream()
                .filter(a -> LocalDateTime.of(a.getData(), a.getHorario()).isAfter(agora))
                .toList();

        afetados.forEach(a -> a.setStatus(AgendamentoStatus.CANCELADO));
        agendamentoRepository.saveAll(afetados);

        int antes = barbeiro.getHorariosTrabalho().size();
        barbeiro.getHorariosTrabalho().removeIf(h -> {
            LocalDate dia = h.toLocalDate();
            return !dia.isBefore(inicio) && !dia.isAfter(fim);
        });
        int removidos = antes - barbeiro.getHorariosTrabalho().size();
        barbeiroRepository.save(barbeiro);

        List<AvisoCancelamento> avisos = afetados.stream()
                .map(a -> new AvisoCancelamento(
                        a.getCliente().getUsuario().getEmail(),
                        a.getCliente().getNome(),
                        barbeiro.getNome(),
                        a.getServico().getNome(),
                        a.getData(),
                        a.getHorario(),
                        request.motivo()))
                .toList();

        if (!avisos.isEmpty()) {
            eventPublisher.publishEvent(new AgendamentosCanceladosEvent(avisos));
        }

        return new CancelamentoResponse(afetados.size(), removidos);
    }

    public void delete(Long id) {
        if (!barbeiroRepository.existsById(id)) {
            throw new NotFoundException("Barbeiro não encontrado");
        }
        try {
            barbeiroRepository.deleteById(id);
            userRepository.deleteById(id);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Não dá para excluir: o barbeiro tem agendamentos ou chamados.");
        }
    }
}