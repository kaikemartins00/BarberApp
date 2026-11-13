
package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.*;
import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.event.VagaFilaEvent;
import com.kaikeMartins.barberapp_backend.exception.*;
import com.kaikeMartins.barberapp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FilaEsperaService {

    private static final ZoneId FUSO =
            ZoneId.of("America/Sao_Paulo");

    private static final Set<AgendamentoStatus> ATIVOS =
            Set.of(
                    AgendamentoStatus.PENDENTE,
                    AgendamentoStatus.CONFIRMADO
            );

    private final FilaEsperaRepository filaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FilaEsperaResponse entrar(FilaEsperaRequest request) {

        UsuarioEntity usuario = usuarioAutenticado();

        AgendamentoEntity ag = agendamentoRepository
                .findById(request.agendamentoAlternativoId())
                .orElseThrow(() -> new NotFoundException(
                        "Agendamento alternativo não encontrado."
                ));

        if (!ag.getCliente().getId().equals(usuario.getId())) {
            throw new ForbiddenException(
                    "Esse agendamento não pertence a você."
            );
        }

        if (!ATIVOS.contains(ag.getStatus())) {
            throw new BadRequestException(
                    "O agendamento alternativo não está ativo."
            );
        }

        LocalDateTime agora = LocalDateTime.now(FUSO);
        LocalDateTime alternativa = LocalDateTime.of(ag.getData(), ag.getHorario());

        if (!alternativa.isAfter(agora)) {
            throw new BadRequestException("O agendamento alternativo já passou."
            );
        }

        boolean jaTemNoDia = agendamentoRepository.findByClienteId(usuario.getId()).stream()
                .anyMatch(a -> ATIVOS.contains(a.getStatus()) && request.dataPreferida().equals(a.getData()));
        if (jaTemNoDia) {
            throw new BadRequestException("Você já tem um horário marcado nesse dia, por isso não pode entrar na fila dele."
            );
        }

        if (!request.dataPreferida().isBefore(ag.getData())) {
            throw new BadRequestException("A data preferida deve ser anterior à alternativa."
            );
        }

        if (filaRepository.existsByAgendamentoIdAndStatus(ag.getId(), FilaEsperaStatus.AGUARDANDO)) {
            throw new BadRequestException("Você já está na fila desse agendamento."
            );
        }

        BarbeiroEntity barbeiro = barbeiroRepository
                .findById(ag.getBarbeiro().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Barbeiro não encontrado."
                ));

        Set<LocalTime> horarios = Set.copyOf(request.horariosAceitos());

        for (LocalTime horario : horarios) {

            LocalDateTime desejado = LocalDateTime.of(request.dataPreferida(), horario);

            if (!desejado.isAfter(agora)) {
                throw new BadRequestException("A fila não aceita horários passados."
                );
            }

            if (!barbeiro.getHorariosTrabalho().contains(desejado)) {
                throw new BadRequestException("Um dos horários escolhidos não pertence à agenda do barbeiro."
                );
            }

            if (!agendamentoRepository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                    barbeiro.getId(),
                    request.dataPreferida(),
                    horario,
                    AgendamentoStatus.CANCELADO
            )) {
                throw new BadRequestException(
                        "Existe um horário livre na data desejada. Faça o agendamento diretamente."
                );
            }
        }

        FilaEsperaEntity fila = FilaEsperaEntity.builder()
                .agendamento(ag)
                .dataPreferida(request.dataPreferida())
                .horariosAceitos(horarios)
                .status(FilaEsperaStatus.AGUARDANDO)
                .criadoEm(agora)
                .build();

        return toResponse(filaRepository.save(fila));
    }

    @Transactional(readOnly = true)
    public FilaEsperaResponse consultar(Long id) {

        UsuarioEntity usuario = usuarioAutenticado();

        FilaEsperaEntity fila = filaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Inscrição na fila não encontrada."
                ));

        AgendamentoEntity ag = fila.getAgendamento();

        boolean cliente = ag.getCliente().getId()
                .equals(usuario.getId());

        boolean barbeiro = temRole(usuario, "ROLE_BARBEIRO")
                && ag.getBarbeiro().getId()
                .equals(usuario.getId());

        if (!cliente && !barbeiro
                && !temRole(usuario, "ROLE_ADMIN")) {
            throw new ForbiddenException(
                    "Você não pode consultar essa fila."
            );
        }

        return toResponse(fila);
    }

    @Transactional
    public FilaEsperaResponse sair(Long id) {

        UsuarioEntity usuario = usuarioAutenticado();

        FilaEsperaEntity fila = filaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Inscrição não encontrada."
                ));

        if (!fila.getAgendamento().getCliente().getId()
                .equals(usuario.getId())) {
            throw new ForbiddenException(
                    "Essa inscrição não pertence a você."
            );
        }

        if (fila.getStatus() != FilaEsperaStatus.AGUARDANDO) {
            throw new BadRequestException(
                    "Essa inscrição não está mais aguardando."
            );
        }

        fila.setStatus(FilaEsperaStatus.CANCELADO);

        return toResponse(filaRepository.save(fila));
    }

    @Transactional
    public void processarCancelamento(AgendamentoEntity cancelado) {


        filaRepository.findByAgendamentoIdAndStatus(
                cancelado.getId(),
                FilaEsperaStatus.AGUARDANDO
        ).forEach(fila -> {
            fila.setStatus(FilaEsperaStatus.CANCELADO);
            filaRepository.save(fila);
        });

        LocalDate data = cancelado.getData();
        LocalTime horario = cancelado.getHorario();
        LocalDateTime vaga = LocalDateTime.of(data, horario);

        if (!vaga.isAfter(LocalDateTime.now(FUSO))) {
            return;
        }

        List<FilaEsperaEntity> candidatos =
                filaRepository.buscarFila(
                        cancelado.getBarbeiro().getId(),
                        data,
                        FilaEsperaStatus.AGUARDANDO
                );

        for (FilaEsperaEntity fila : candidatos) {

            if (!fila.getHorariosAceitos().contains(horario)) {
                continue;
            }

            AgendamentoEntity alternativa = fila.getAgendamento();

            if (!ATIVOS.contains(alternativa.getStatus())) {
                continue;
            }

            if (!LocalDateTime.of(alternativa.getData(), alternativa.getHorario()).isAfter(LocalDateTime.now(FUSO))) {
                continue;
            }

            if (!alternativa.getBarbeiro().getId().equals(cancelado.getBarbeiro().getId())) {
                continue;
            }

            Optional<PagamentoEntity> pagamento = pagamentoRepository.findByAgendamentoId(alternativa.getId());

            if (pagamento.isEmpty() || pagamento.get().getStatus() == PagamentoStatus.CANCELADO) {
                continue;
            }

            if (agendamentoRepository.existsByBarbeiroIdAndDataAndHorarioAndStatusNot(
                    cancelado.getBarbeiro().getId(),
                    data,
                    horario,
                    AgendamentoStatus.CANCELADO
            )) {
                return;
            }

            LocalDate dataAnterior = alternativa.getData();
            LocalTime horarioAnterior = alternativa.getHorario();

            alternativa.setData(data);
            alternativa.setHorario(horario);

            agendamentoRepository.saveAndFlush(alternativa);

            fila.setStatus(FilaEsperaStatus.ATENDIDO);
            fila.setAtendidoEm(LocalDateTime.now(FUSO));
            filaRepository.save(fila);

            eventPublisher.publishEvent(new VagaFilaEvent(
                    alternativa.getCliente().getUsuario().getEmail(),
                    alternativa.getCliente().getNome(),
                    alternativa.getBarbeiro().getNome(),
                    alternativa.getServico().getNome(),
                    dataAnterior,
                    horarioAnterior,
                    data,
                    horario
            ));

            return;
        }
    }

    private FilaEsperaResponse toResponse(FilaEsperaEntity fila) {
        AgendamentoEntity ag = fila.getAgendamento();

        return new FilaEsperaResponse(
                fila.getId(),
                ag.getId(),
                fila.getDataPreferida(),
                fila.getHorariosAceitos(),
                ag.getData(),
                ag.getHorario(),
                fila.getStatus().name()
        );
    }

    private UsuarioEntity usuarioAutenticado() {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UsuarioEntity usuario) {
            return usuario;
        }

        throw new ForbiddenException("Usuário não autenticado.");
    }

    private boolean temRole(UsuarioEntity usuario, String role) {
        return usuario.getRoles().stream().anyMatch(r -> role.equals(r.getAuthority()));
    }
}
