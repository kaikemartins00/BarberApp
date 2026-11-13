package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroDesligadoResponse;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroResponse;
import com.kaikeMartins.barberapp_backend.dto.DesligarBarbeiroRequest;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroUpdateRequest;
import com.kaikeMartins.barberapp_backend.dto.CancelamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.CancelarDiaRequest;
import com.kaikeMartins.barberapp_backend.dto.HorariosRequest;
import com.kaikeMartins.barberapp_backend.dto.ServicosIdsRequest;
import com.kaikeMartins.barberapp_backend.event.AgendamentosCanceladosEvent;
import com.kaikeMartins.barberapp_backend.event.AvisoCancelamento;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final PagamentoService pagamentoService;

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
        return barbeiroRepository.findAll().stream()
                .filter(b -> b.getDesligadoEm() == null)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BarbeiroDesligadoResponse> listarDesligados() {
        return barbeiroRepository.findAll().stream()
                .filter(b -> b.getDesligadoEm() != null)
                .sorted(java.util.Comparator.comparing(BarbeiroEntity::getDesligadoEm).reversed())
                .map(b -> new BarbeiroDesligadoResponse(
                        b.getId(), b.getNome(), b.getUsuario().getEmail(),
                        b.getDesligadoEm(), b.getMotivoDesligamento()))
                .toList();
    }

    /**
     * Desliga o barbeiro sem apagar nada: guarda o motivo, bloqueia o acesso, limpa a agenda futura
     * e cancela (com reembolso e e-mail) os atendimentos que ele ainda tinha marcados.
     */
    public CancelamentoResponse desligar(Long id, DesligarBarbeiroRequest request) {
        BarbeiroEntity barbeiro = buscar(id);
        if (barbeiro.getDesligadoEm() != null) {
            throw new BadRequestException("Este barbeiro já foi desligado.");
        }

        LocalDateTime agora = LocalDateTime.now();

        List<AgendamentoEntity> afetados = agendamentoRepository
                .findByBarbeiroIdAndDataBetweenAndStatusIn(id, agora.toLocalDate(), agora.toLocalDate().plusYears(2),
                        List.of(AgendamentoStatus.PENDENTE, AgendamentoStatus.CONFIRMADO))
                .stream()
                .filter(a -> LocalDateTime.of(a.getData(), a.getHorario()).isAfter(agora))
                .toList();

        afetados.forEach(a -> a.setStatus(AgendamentoStatus.CANCELADO));
        agendamentoRepository.saveAll(afetados);
        afetados.forEach(pagamentoService::reembolsarPorCancelamento);

        int removidos = barbeiro.getHorariosTrabalho().size();
        barbeiro.getHorariosTrabalho().clear();
        barbeiro.setDesligadoEm(agora);
        barbeiro.setMotivoDesligamento(request.motivo().trim());
        barbeiroRepository.save(barbeiro);

        UsuarioEntity usuario = barbeiro.getUsuario();
        usuario.setVerificado(false); // sem acesso ao sistema
        userRepository.save(usuario);

        List<AvisoCancelamento> avisos = afetados.stream()
                .map(a -> new AvisoCancelamento(
                        a.getCliente().getUsuario().getEmail(),
                        a.getCliente().getNome(),
                        barbeiro.getNome(),
                        a.getServico().getNome(),
                        a.getData(),
                        a.getHorario(),
                        "O profissional não faz mais parte da equipe da barbearia."))
                .toList();

        if (!avisos.isEmpty()) {
            eventPublisher.publishEvent(new AgendamentosCanceladosEvent(avisos));
        }

        return new CancelamentoResponse(afetados.size(), removidos);
    }

    @Transactional(readOnly = true)
    public BarbeiroResponse findById(Long id) {
        return toResponse(buscar(id));
    }

    public BarbeiroResponse update(Long id, BarbeiroUpdateRequest request) {
        BarbeiroEntity barbeiro = buscar(id);
        UsuarioEntity usuario = barbeiro.getUsuario();

        // o próprio barbeiro só pode trocar a foto; e-mail e senha continuam com o admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioEntity autor
                && autor.getRoles().stream().noneMatch(r -> "ROLE_ADMIN".equals(r.getAuthority()))) {
            if (!autor.getId().equals(id)) {
                throw new ForbiddenException("Você só pode alterar o seu próprio perfil.");
            }
            barbeiro.setFotoUrl(request.fotoUrl());
            return toResponse(barbeiroRepository.save(barbeiro));
        }

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

        afetados.forEach(pagamentoService::reembolsarPorCancelamento);

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