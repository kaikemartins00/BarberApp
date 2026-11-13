
package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.OcupacaoResponse;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OcupacaoService {

    private final BarbeiroRepository barbeiroRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public OcupacaoResponse consultar(Long barbeiroId, LocalDate data) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioEntity usuario)) {
            throw new ForbiddenException("Usuário não autenticado");
        }

        boolean admin = usuario.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!admin && !usuario.getId().equals(barbeiroId)) {
            throw new ForbiddenException("Você não pode consultar a ocupação de outro barbeiro");
        }

        BarbeiroEntity barbeiro = barbeiroRepository
                .findById(barbeiroId)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));

        Set<LocalTime> horarios = barbeiro
                .getHorariosTrabalho()
                .stream()
                .filter(h -> h.toLocalDate().equals(data))
                .map(h -> h.toLocalTime())
                .collect(Collectors.toSet());

        Set<LocalTime> ocupados = agendamentoRepository
                .findByBarbeiroIdAndData(barbeiroId, data)
                .stream()
                .filter(a ->
                        a.getStatus() != AgendamentoStatus.CANCELADO
                )
                .map(a -> a.getHorario())
                .filter(horarios::contains)
                .collect(Collectors.toSet());

        int total = horarios.size();
        int reservados = ocupados.size();
        int livres = Math.max(0, total - reservados);

        double percentual = total == 0
                ? 0
                : Math.round(reservados * 10000.0 / total) / 100.0;

        return new OcupacaoResponse(
                barbeiroId,
                data,
                total,
                reservados,
                livres,
                percentual
        );
    }
}
