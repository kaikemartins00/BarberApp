package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ConversaEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.MensagemEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.dto.ConversaRequest;
import com.kaikeMartins.barberapp_backend.dto.ConversaResponse;
import com.kaikeMartins.barberapp_backend.dto.MensagemRequest;
import com.kaikeMartins.barberapp_backend.dto.MensagemResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.ConversaRepository;
import com.kaikeMartins.barberapp_backend.repository.MensagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversaService {

    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;

    @Transactional(readOnly = true)
    public List<ConversaResponse> minhasConversas() {
        UsuarioEntity usuario = usuarioAutenticado();

        List<ConversaEntity> conversas = temRole(usuario, "ROLE_BARBEIRO")
                ? conversaRepository.findByBarbeiroId(usuario.getId())
                : conversaRepository.findByClienteId(usuario.getId());

        return conversas.stream()
                .map(c -> toResponse(c, usuario.getId()))
                .sorted(Comparator.comparing(ConversaResponse::ultimaMensagemEm,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .toList();
    }

    public ConversaResponse iniciar(ConversaRequest request) {
        UsuarioEntity usuario = usuarioAutenticado();

        ClienteEntity cliente = clienteRepository.findById(usuario.getId())
                .orElseThrow(() -> new ForbiddenException("Apenas clientes podem iniciar conversas."));

        BarbeiroEntity barbeiro = barbeiroRepository.findById(request.barbeiroId())
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));

        ConversaEntity conversa = conversaRepository
                .findByClienteIdAndBarbeiroId(cliente.getId(), barbeiro.getId())
                .orElseGet(() -> conversaRepository.save(
                        ConversaEntity.builder().cliente(cliente).barbeiro(barbeiro).build()));

        if (conversa.isEncerrada()) {
            conversa.setEncerrada(false);
        }

        return toResponse(conversa, usuario.getId());
    }

    @Transactional(readOnly = true)
    public List<MensagemResponse> mensagens(Long conversaId, Long depoisDe) {
        UsuarioEntity usuario = usuarioAutenticado();
        buscarComoParticipante(conversaId, usuario.getId());

        return mensagemRepository
                .findByConversaIdAndIdGreaterThanOrderByIdAsc(conversaId, depoisDe == null ? 0L : depoisDe)
                .stream()
                .map(m -> toResponse(m, usuario.getId()))
                .toList();
    }

    public MensagemResponse enviar(Long conversaId, MensagemRequest request) {
        UsuarioEntity usuario = usuarioAutenticado();
        ConversaEntity conversa = buscarComoParticipante(conversaId, usuario.getId());

        if (conversa.isEncerrada()) {
            throw new BadRequestException("Essa conversa foi finalizada.");
        }

        MensagemEntity mensagem = mensagemRepository.save(MensagemEntity.builder()
                .conversa(conversa)
                .remetente(usuario)
                .texto(request.texto().trim())
                .build());

        return toResponse(mensagem, usuario.getId());
    }

    public void marcarComoLidas(Long conversaId) {
        UsuarioEntity usuario = usuarioAutenticado();
        buscarComoParticipante(conversaId, usuario.getId());
        mensagemRepository.marcarComoLidas(conversaId, usuario.getId());
    }

    public ConversaResponse finalizar(Long conversaId) {
        UsuarioEntity usuario = usuarioAutenticado();
        ConversaEntity conversa = buscarComoParticipante(conversaId, usuario.getId());

        if (conversa.isEncerrada()) {
            throw new BadRequestException("Essa conversa já foi finalizada.");
        }

        conversa.setEncerrada(true);
        return toResponse(conversa, usuario.getId());
    }

    public ConversaResponse reabrir(Long conversaId) {
        UsuarioEntity usuario = usuarioAutenticado();
        ConversaEntity conversa = buscarComoParticipante(conversaId, usuario.getId());

        if (!conversa.getCliente().getId().equals(usuario.getId())) {
            throw new ForbiddenException("Só o cliente pode reabrir a conversa.");
        }

        conversa.setEncerrada(false);
        return toResponse(conversa, usuario.getId());
    }

    private ConversaEntity buscarComoParticipante(Long conversaId, Long usuarioId) {
        ConversaEntity conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada"));

        boolean participa = conversa.getCliente().getId().equals(usuarioId)
                || conversa.getBarbeiro().getId().equals(usuarioId);

        if (!participa) {
            throw new ForbiddenException("Você não participa dessa conversa.");
        }
        return conversa;
    }

    private ConversaResponse toResponse(ConversaEntity conversa, Long usuarioId) {
        String nomeDoOutro = conversa.getCliente().getId().equals(usuarioId)
                ? conversa.getBarbeiro().getNome()
                : conversa.getCliente().getNome();

        Optional<MensagemEntity> ultima = mensagemRepository.findFirstByConversaIdOrderByIdDesc(conversa.getId());
        long naoLidas = mensagemRepository
                .countByConversaIdAndRemetenteIdNotAndLidaFalse(conversa.getId(), usuarioId);

        return new ConversaResponse(
                conversa.getId(),
                nomeDoOutro,
                ultima.map(MensagemEntity::getTexto).orElse(null),
                ultima.map(MensagemEntity::getEnviadaEm).orElse(null),
                naoLidas,
                conversa.isEncerrada());
    }

    private MensagemResponse toResponse(MensagemEntity mensagem, Long usuarioId) {
        return new MensagemResponse(
                mensagem.getId(),
                mensagem.getTexto(),
                mensagem.getEnviadaEm(),
                mensagem.getRemetente().getId().equals(usuarioId),
                mensagem.isLida());
    }

    private UsuarioEntity usuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UsuarioEntity usuario) {
            return usuario;
        }
        throw new ForbiddenException("Usuário não autenticado ou sessão inválida.");
    }

    private boolean temRole(UsuarioEntity usuario, String role) {
        return usuario.getRoles().stream().anyMatch(r -> role.equals(r.getAuthority()));
    }


}