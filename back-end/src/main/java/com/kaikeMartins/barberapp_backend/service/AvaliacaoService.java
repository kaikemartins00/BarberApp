package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.exception.*;
import com.kaikeMartins.barberapp_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository repository;
    private final AgendamentoRepository agendamentos;
    private final BarbeiroRepository barbeiros;

    private UsuarioEntity usuario() {
        return (UsuarioEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private AvaliacaoResponse resposta(AvaliacaoEntity a) {
        return new AvaliacaoResponse(a.getId(),
                a.getAgendamento().getId(),
                a.getAgendamento().getBarbeiro().getId(),
                a.getNota(),a.getComentario(),
                a.getTags()==null || a.getTags().isBlank()? List.of():List.of(a.getTags().split(",")),a.getCriadaEm());
    }

    @Transactional
    public AvaliacaoResponse criar(AvaliacaoRequest request) {

        AgendamentoEntity ag = agendamentos.findById(request.agendamentoId())
                .orElseThrow(()->
                        new NotFoundException("Agendamento não encontrado."));

        if (!ag.getCliente().getId().equals(usuario().getId()))
            throw new ForbiddenException("Agendamento não pertence ao cliente.");
        if (ag.getStatus()!=AgendamentoStatus.CONCLUIDO) throw new BadRequestException("Somente atendimentos concluídos podem ser avaliados.");

        if (request.nota()<1 || request.nota()>5) throw new BadRequestException("A nota deve estar entre 1 e 5.");

        if (repository.existsByAgendamentoId(ag.getId())) throw new BadRequestException("Esse atendimento já foi avaliado.");

        List<String> tags = request.caracteristicas()==null?List.of():request.caracteristicas();

        if (String.join(",",tags).length()>250 || tags.size()>5 || tags.stream()
                .anyMatch(t->t==null || t.isBlank() || t.contains(",") || t.length()>40))
            throw new BadRequestException("Tags inválidas.");

        AvaliacaoEntity a = AvaliacaoEntity.builder()
                .agendamento(ag)
                .nota(request.nota())
                .comentario(request.comentario())
                .tags(String.join(",",tags))
                .criadaEm(LocalDateTime.now()).build();

        return resposta(repository.save(a));
    }

    @Transactional(readOnly=true)
    public boolean porAgendamento(Long id) {

        AgendamentoEntity ag = agendamentos.findById(id).orElseThrow(()->new NotFoundException("Agendamento não encontrado."));

        UsuarioEntity u=usuario();
        boolean admin=u.getAuthorities().stream().anyMatch(r->r.getAuthority().equals("ROLE_ADMIN"));

        if (!admin && !ag.getCliente().getId().equals(u.getId())
                && !ag.getBarbeiro().getId().equals(u.getId())) {
            throw new ForbiddenException("Acesso negado.");
        }

        return repository.existsByAgendamentoId(id);
    }
    @Transactional(readOnly=true)
    public AvaliacaoResumoResponse resumo(Long id) {

        if (!barbeiros.existsById(id)) throw new NotFoundException("Barbeiro não encontrado.");
        List<AvaliacaoEntity> lista=repository.findByAgendamentoBarbeiroId(id);
        return new AvaliacaoResumoResponse(lista.stream().mapToInt(AvaliacaoEntity::getNota).average().orElse(0),lista.size());
    }
}
