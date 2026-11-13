package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.dto.NotaAgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.PreferenciasClienteRequest;
import com.kaikeMartins.barberapp_backend.dto.PreferenciasClienteResponse;
import com.kaikeMartins.barberapp_backend.dto.ServicoPopularResponse;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.AvaliacaoRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PainelClienteService {

    private static final int LIMITE_SERVICOS_POPULARES = 3;

    private final ClienteRepository clienteRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public List<NotaAgendamentoResponse> notasDoCliente() {
        ClienteEntity cliente = clienteAutenticado();

        return avaliacaoRepository.findByAgendamentoClienteId(cliente.getId()).stream()
                .map(a -> new NotaAgendamentoResponse(a.getAgendamento().getId(), a.getNota()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicoPopularResponse> servicosMaisFeitos() {
        return agendamentoRepository.servicosMaisFeitos().stream()
                .limit(LIMITE_SERVICOS_POPULARES)
                .map(linha -> new ServicoPopularResponse((String) linha[0], ((Number) linha[1]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PreferenciasClienteResponse preferencias() {
        ClienteEntity cliente = clienteAutenticado();

        return new PreferenciasClienteResponse(textoOuVazio(cliente.getPreferencias()));
    }

    @Transactional
    public PreferenciasClienteResponse salvarPreferencias(PreferenciasClienteRequest request) {
        ClienteEntity cliente = clienteAutenticado();

        String texto = request.observacoes() == null ? "" : request.observacoes().trim();
        cliente.setPreferencias(texto.isEmpty() ? null : texto);
        clienteRepository.save(cliente);

        return new PreferenciasClienteResponse(texto);
    }

    private String textoOuVazio(String texto) {
        return texto == null ? "" : texto;
    }

    private ClienteEntity clienteAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UsuarioEntity usuario) {
            return clienteRepository.findById(usuario.getId())
                    .orElseThrow(() -> new ForbiddenException("Apenas clientes podem usar esta função."));
        }

        throw new ForbiddenException("Usuário não autenticado ou sessão inválida.");
    }
}
