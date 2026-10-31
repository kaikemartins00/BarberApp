package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;
import com.kaikeMartins.barberapp_backend.dto.ChamadoRequest;
import com.kaikeMartins.barberapp_backend.dto.ChamadoResponse;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamRequest;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ChamadoRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.RespostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final RespostaRepository respostaRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;

    private ChamadoResponse toResponse(ChamadoEntity c, Long usuarioId) {
        List<RespostaChamResponse> respostas = c.getRespostas().stream()
                .sorted(Comparator.comparing(RespostaChamadoEntity::getDataEnvio))
                .map(r -> new RespostaChamResponse(
                        r.getId(),
                        r.getRespostaMsg(),
                        r.getDataEnvio(),
                        r.getBarbeiro() != null ? r.getBarbeiro().getNome() : "Administração"))
                .toList();

        UsuarioEntity responsavel = c.getResponsavel();
        boolean podeAtender = c.getStatus() != ChamadoStatus.FINALIZADO
                && (responsavel == null || responsavel.getId().equals(usuarioId));

        return new ChamadoResponse(
                c.getId(), c.getTitulo(), c.getDescricao(), c.getChamadoDate(),
                c.getStatus(), c.getPrioridade(),
                c.getCliente().getNome(),
                c.getCliente().getUsuario().getEmail(),
                nomeDoResponsavel(responsavel),
                podeAtender,
                respostas);
    }

    /** Admin vê todos. Barbeiro vê os livres e os que ele atende (ou atendeu). */
    @Transactional(readOnly = true)
    public List<ChamadoResponse> findAll() {
        UsuarioEntity usuario = usuarioAutenticado();
        boolean admin = ehAdmin(usuario);

        return chamadoRepository.findAllByOrderByChamadoDateDesc().stream()
                .filter(c -> admin || visivelPara(c, usuario.getId()))
                .map(c -> toResponse(c, usuario.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> meus() {
        UsuarioEntity usuario = usuarioAutenticado();
        return chamadoRepository.findByClienteIdOrderByChamadoDateDesc(clienteAutenticado().getId())
                .stream().map(c -> toResponse(c, usuario.getId())).toList();
    }

    public ChamadoResponse create(ChamadoRequest request) {
        ChamadoEntity entity = ChamadoEntity.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .cliente(clienteAutenticado())
                .status(ChamadoStatus.ABERTO)
                .prioridade(getPrioridade(request))
                .build();
        return toResponse(chamadoRepository.save(entity), usuarioAutenticado().getId());
    }

    /** Assume o chamado. Admin pode tomar um chamado que está com outra pessoa. */
    public ChamadoResponse assumir(Long id) {
        UsuarioEntity usuario = usuarioAutenticado();
        ChamadoEntity chamado = buscarParaAtualizar(id);

        if (chamado.getStatus() == ChamadoStatus.FINALIZADO) {
            throw new BadRequestException("Esse chamado já foi finalizado.");
        }

        if (!ehAdmin(usuario)) {
            exigirResponsavel(chamado, usuario);
        }

        assumirPara(chamado, usuario);
        return toResponse(chamado, usuario.getId());
    }

    public ChamadoResponse responder(Long id, RespostaChamRequest request) {
        UsuarioEntity usuario = usuarioAutenticado();
        ChamadoEntity chamado = buscarParaAtualizar(id);

        if (chamado.getStatus() == ChamadoStatus.FINALIZADO) {
            throw new BadRequestException("Esse chamado já foi finalizado.");
        }

        exigirResponsavel(chamado, usuario);
        assumirPara(chamado, usuario);

        BarbeiroEntity barbeiro = barbeiroRepository.findById(usuario.getId()).orElse(null);

        RespostaChamadoEntity resposta = RespostaChamadoEntity.builder()
                .respostaMsg(request.respostaMsg())
                .barbeiro(barbeiro)
                .chamado(chamado)
                .build();

        respostaRepository.save(resposta);
        chamado.getRespostas().add(resposta);
        return toResponse(chamado, usuario.getId());
    }

    public ChamadoResponse finalizar(Long id) {
        UsuarioEntity usuario = usuarioAutenticado();
        ChamadoEntity chamado = buscarParaAtualizar(id);

        if (chamado.getStatus() == ChamadoStatus.FINALIZADO) {
            throw new BadRequestException("Esse chamado já foi finalizado.");
        }

        exigirResponsavel(chamado, usuario);
        assumirPara(chamado, usuario);

        chamado.setStatus(ChamadoStatus.FINALIZADO);
        return toResponse(chamado, usuario.getId());
    }

    // ---------- regras de visibilidade e de responsável ----------

    private boolean visivelPara(ChamadoEntity chamado, Long usuarioId) {
        UsuarioEntity responsavel = chamado.getResponsavel();
        boolean ehDele = responsavel != null && responsavel.getId().equals(usuarioId);

        if (chamado.getStatus() == ChamadoStatus.FINALIZADO) {
            return ehDele;
        }
        return responsavel == null || ehDele;
    }

    private void assumirPara(ChamadoEntity chamado, UsuarioEntity usuario) {
        chamado.setResponsavel(usuario);
        chamado.setBarbeiro(barbeiroRepository.findById(usuario.getId()).orElse(null));
        if (chamado.getStatus() == ChamadoStatus.ABERTO) {
            chamado.setStatus(ChamadoStatus.EM_ANDAMENTO);
        }
    }

    /** Barra quem não é o responsável. Chamado sem responsável fica livre para quem chegar primeiro. */
    private void exigirResponsavel(ChamadoEntity chamado, UsuarioEntity usuario) {
        UsuarioEntity atual = chamado.getResponsavel();
        if (atual != null && !atual.getId().equals(usuario.getId())) {
            throw new ForbiddenException(
                    "Esse chamado já está sendo atendido por " + nomeDoResponsavel(atual) + ".");
        }
    }

    private String nomeDoResponsavel(UsuarioEntity usuario) {
        if (usuario == null) return null;
        return barbeiroRepository.findById(usuario.getId())
                .map(BarbeiroEntity::getNome)
                .orElse("Administração");
    }

    // ---------- apoio ----------

    private ChamadoEntity buscarParaAtualizar(Long id) {
        return chamadoRepository.buscarParaAtualizar(id)
                .orElseThrow(() -> new NotFoundException("Chamado não existe"));
    }

    private ClienteEntity clienteAutenticado() {
        UsuarioEntity usuario = usuarioAutenticado();
        return clienteRepository.findById(usuario.getId())
                .orElseThrow(() -> new ForbiddenException("Apenas clientes podem abrir chamados ou gerenciar sua conta nesta área."));
    }

    private UsuarioEntity usuarioAutenticado() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof UsuarioEntity usuario) {
            return usuario;
        }
        throw new ForbiddenException("Usuário não autenticado ou sessão inválida.");
    }

    private boolean ehAdmin(UsuarioEntity usuario) {
        return usuario.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getAuthority()));
    }

    private PrioridadeChamado getPrioridade(ChamadoRequest request) {
        String titulo = Optional.ofNullable(request.titulo()).orElse("").toLowerCase();
        String descricao = Optional.ofNullable(request.descricao()).orElse("").toLowerCase();

        String textoCompleto = titulo + " " + descricao;

        PrioridadeChamado prioridade = PrioridadeChamado.BAIXA;

        if (textoCompleto.contains("servidor") || textoCompleto.contains("sistema fora")) {
            prioridade = PrioridadeChamado.ALTA;
        } else if (textoCompleto.contains("lento") || textoCompleto.contains("bug")) {
            prioridade = PrioridadeChamado.MEDIA;
        }

        return prioridade;
    }
}