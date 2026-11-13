package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.dto.ClienteBloqueadoResponse;
import com.kaikeMartins.barberapp_backend.event.ClienteDesbloqueadoEvent;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BloqueioService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ClienteRepository clienteRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ClienteBloqueadoResponse> listarBloqueados() {
        LocalDateTime agora = LocalDateTime.now(FUSO);

        return clienteRepository.findAll().stream()
                .filter(c -> c.getBloqueadoAte() != null && c.getBloqueadoAte().isAfter(agora))
                .sorted(Comparator.comparing(ClienteEntity::getBloqueadoAte))
                .map(c -> new ClienteBloqueadoResponse(
                        c.getId(),
                        c.getNome(),
                        c.getUsuario().getEmail(),
                        c.getTelefone(),
                        c.getBloqueadoAte()
                ))
                .toList();
    }

    @Transactional
    public void desbloquear(Long clienteId) {
        ClienteEntity cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));

        cliente.setBloqueadoAte(null);
        cliente.setDesbloqueadoEm(LocalDateTime.now(FUSO));
        clienteRepository.save(cliente);

        eventPublisher.publishEvent(new ClienteDesbloqueadoEvent(
                cliente.getUsuario().getEmail(),
                cliente.getNome(),
                AgendamentoService.LIMITE_CANCELAMENTOS_MES
        ));
    }
}
