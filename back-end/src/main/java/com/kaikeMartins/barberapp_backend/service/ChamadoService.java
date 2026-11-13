package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoNotificacaoTipo;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;
import com.kaikeMartins.barberapp_backend.dto.ChamadoRequest;
import com.kaikeMartins.barberapp_backend.dto.ChamadoResponse;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamRequest;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamResponse;
import com.kaikeMartins.barberapp_backend.event.ChamadoNotificacaoEvent;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ChamadoRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.RespostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final RespostaRepository respostaRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AgendamentoRepository agendamentoRepository;
    private final PagamentoService pagamentoService;

    // Quanto cada assunto pesa na prioridade (o impacto do problema, não as palavras usadas)
    private static final Map<String, Integer> PESO_ASSUNTO = Map.of(
            "Erro no pagamento ou Pix", 70,
            "Site fora do ar ou muito lento", 60,
            "Não consegui agendar", 55,
            "Problema com meu agendamento ou reembolso", 50,
            "Problema com o código de verificação", 45,
            "Erro no site", 35,
            "Outro", 10);
    private static final int PESO_PADRAO = 20;
    // mesmo limite do AgendamentoService: 3 cancelamentos do cliente por mês
    private static final int LIMITE_CANCELAMENTOS_MES = 3;
    // sinais no texto são só um reforço pequeno: quem não sabe dizer "servidor" não perde prioridade
    private static final List<String> SINAIS = List.of("servidor", "fora do ar", "não abre", "nao abre",
            "não carrega", "nao carrega", "travou", "caiu", "cobrado", "debitou", "dinheiro", "pix");

    private ChamadoResponse toResponse(ChamadoEntity c, Long usuarioId) {
        return toResponse(c, usuarioId, c.getPrioridade());
    }

    private ChamadoResponse toResponse(ChamadoEntity c, Long usuarioId, PrioridadeChamado prioridade) {
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

        AgendamentoEntity ag = c.getAgendamento();
        boolean reembolsavel = ag != null
                && c.getStatus() != ChamadoStatus.FINALIZADO
                && ag.getStatus() == AgendamentoStatus.CANCELADO
                && pagamentoService.podeReembolsar(ag);

        return new ChamadoResponse(
                c.getId(), c.getTitulo(), c.getDescricao(), c.getChamadoDate(),
                c.getStatus(), prioridade,
                c.getCliente().getNome(),
                c.getCliente().getUsuario().getEmail(),
                nomeDoResponsavel(responsavel),
                podeAtender,
                respostas,
                ag != null ? ag.getId() : null,
                reembolsavel);
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> findAll() {
        UsuarioEntity usuario = usuarioAutenticado();
        boolean admin = ehAdmin(usuario);

        List<ChamadoEntity> todos = chamadoRepository.findAllByOrderByChamadoDateDesc();
        List<ChamadoEntity> pendentes = todos.stream()
                .filter(c -> c.getStatus() != ChamadoStatus.FINALIZADO).toList();

        Map<Long, Integer> pontos = new HashMap<>();
        pendentes.forEach(c -> pontos.put(c.getId(), pontuar(c, pendentes)));

        // pendentes primeiro, do mais grave ao menos grave (empate: o mais antigo); finalizados por último
        return todos.stream()
                .filter(c -> admin || visivelPara(c, usuario.getId()))
                .sorted(Comparator
                        .comparing((ChamadoEntity c) -> c.getStatus() == ChamadoStatus.FINALIZADO)
                        .thenComparing(c -> -pontos.getOrDefault(c.getId(), 0))
                        .thenComparing(ChamadoEntity::getChamadoDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(c -> toResponse(c, usuario.getId(),
                        pontos.containsKey(c.getId()) ? nivel(pontos.get(c.getId())) : c.getPrioridade()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> meus() {
        UsuarioEntity usuario = usuarioAutenticado();
        return chamadoRepository.findByClienteIdOrderByChamadoDateDesc(clienteAutenticado().getId())
                .stream().map(c -> toResponse(c, usuario.getId())).toList();
    }

    public ChamadoResponse create(ChamadoRequest request) {
        ClienteEntity cliente = clienteAutenticado();
        AgendamentoEntity agendamento = null;
        if (request.agendamentoId() != null) {
            agendamento = agendamentoRepository.findById(request.agendamentoId())
                    .filter(a -> a.getCliente().getId().equals(cliente.getId()))
                    .orElseThrow(() -> new NotFoundException("Agendamento não encontrado."));
            if (agendamento.getStatus() != AgendamentoStatus.CANCELADO) {
                throw new BadRequestException("Só é possível pedir análise de um agendamento cancelado.");
            }
        }

        ChamadoEntity entity = ChamadoEntity.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .agendamento(agendamento)
                .cliente(cliente)
                .status(ChamadoStatus.ABERTO)
                .prioridade(getPrioridade(request))
                .build();
        ChamadoEntity salvo = chamadoRepository.save(entity);
        notificar(ChamadoNotificacaoTipo.ABERTO, salvo, salvo.getDescricao(), null);
        return toResponse(salvo, usuarioAutenticado().getId());
    }

    public ChamadoResponse assumir(Long id) {
        UsuarioEntity usuario = usuarioAutenticado();
        ChamadoEntity chamado = buscarParaAtualizar(id);

        if (chamado.getStatus() == ChamadoStatus.FINALIZADO) {
            throw new BadRequestException("Esse chamado já foi finalizado.");
        }

        if (!ehAdmin(usuario)) {
            exigirResponsavel(chamado, usuario);
        }

        UsuarioEntity anterior = chamado.getResponsavel();
        assumirPara(chamado, usuario);
        if (anterior == null || !anterior.getId().equals(usuario.getId())) {
            notificar(ChamadoNotificacaoTipo.ASSUMIDO, chamado, null, nomeDoResponsavel(usuario));
        }
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
        notificar(ChamadoNotificacaoTipo.RESPONDIDO, chamado, request.respostaMsg(), nomeDoResponsavel(usuario));
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
        notificar(ChamadoNotificacaoTipo.FINALIZADO, chamado, null, nomeDoResponsavel(usuario));
        return toResponse(chamado, usuario.getId());
    }

    /**
     * Exceção aprovada pelo administrador: devolve o Pix de um agendamento cancelado fora do prazo,
     * não conta esse cancelamento no limite mensal do cliente e avisa o cliente no próprio chamado.
     */
    public ChamadoResponse reembolsar(Long id) {
        UsuarioEntity usuario = usuarioAutenticado();
        if (!ehAdmin(usuario)) {
            throw new ForbiddenException("Só a administração pode aprovar reembolsos.");
        }

        ChamadoEntity chamado = buscarParaAtualizar(id);
        AgendamentoEntity ag = chamado.getAgendamento();

        if (chamado.getStatus() == ChamadoStatus.FINALIZADO) {
            throw new BadRequestException("Esse chamado já foi finalizado.");
        }
        if (ag == null || ag.getStatus() != AgendamentoStatus.CANCELADO) {
            throw new BadRequestException("Esse chamado não tem um agendamento cancelado para reembolsar.");
        }
        exigirResponsavel(chamado, usuario); // antes de mexer no dinheiro
        if (!pagamentoService.reembolsarPorFalta(ag)) {
            throw new BadRequestException("Não há Pix pago para reembolsar nesse agendamento (ou ele já foi reembolsado).");
        }

        // o cancelamento aprovado como exceção não pesa no limite mensal
        ag.setCanceladoPeloCliente(false);
        agendamentoRepository.save(ag);
        ClienteEntity cliente = ag.getCliente();
        LocalDate hoje = LocalDate.now();
        long restantes = agendamentoRepository.countByClienteIdAndCanceladoPeloClienteTrueAndCanceladoEmBetween(
                cliente.getId(),
                hoje.withDayOfMonth(1).atStartOfDay(),
                hoje.withDayOfMonth(hoje.lengthOfMonth()).atTime(LocalTime.MAX));
        if (cliente.getBloqueadoAte() != null && restantes < LIMITE_CANCELAMENTOS_MES) {
            cliente.setBloqueadoAte(null);
            clienteRepository.save(cliente);
        }

        assumirPara(chamado, usuario);
        String aviso = "Reembolso aprovado e efetuado. O comprovante foi enviado para o seu e-mail.";
        RespostaChamadoEntity resposta = RespostaChamadoEntity.builder()
                .respostaMsg(aviso)
                .barbeiro(barbeiroRepository.findById(usuario.getId()).orElse(null))
                .chamado(chamado)
                .build();
        respostaRepository.save(resposta);
        chamado.getRespostas().add(resposta);
        notificar(ChamadoNotificacaoTipo.RESPONDIDO, chamado, aviso, nomeDoResponsavel(usuario));
        return toResponse(chamado, usuario.getId());
    }

    private void notificar(ChamadoNotificacaoTipo tipo, ChamadoEntity chamado, String texto, String atendente) {
        eventPublisher.publishEvent(new ChamadoNotificacaoEvent(
                tipo,
                chamado.getId(),
                chamado.getCliente().getUsuario().getEmail(),
                chamado.getCliente().getNome(),
                chamado.getTitulo(),
                texto,
                atendente));
    }

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
        return nivel(pesoInicial(request.titulo(), request.descricao()));
    }

    private int pesoInicial(String titulo, String descricao) {
        String texto = (Optional.ofNullable(titulo).orElse("") + " "
                + Optional.ofNullable(descricao).orElse("")).toLowerCase();
        int base = PESO_ASSUNTO.getOrDefault(titulo, PESO_PADRAO);
        int sinal = SINAIS.stream().anyMatch(texto::contains) ? 10 : 0;
        return base + sinal;
    }

    /**
     * Pontuação = impacto do assunto + sinais no texto + tempo esperando + volume de clientes
     * com o mesmo problema nas últimas 24h (vários clientes iguais indicam falha do sistema).
     * Chamado esquecido sobe sozinho: 0,15 ponto por hora, até 80 (cerca de 22 dias).
     */
    private int pontuar(ChamadoEntity c, List<ChamadoEntity> pendentes) {
        LocalDateTime agora = LocalDateTime.now();
        int pontos = pesoInicial(c.getTitulo(), c.getDescricao());

        if (c.getChamadoDate() != null) {
            pontos += (int) Math.min(80, Duration.between(c.getChamadoDate(), agora).toHours() * 0.15);
        }

        long clientesComOMesmoProblema = pendentes.stream()
                .filter(o -> !o.getId().equals(c.getId()))
                .filter(o -> o.getTitulo().equals(c.getTitulo()))
                .filter(o -> o.getChamadoDate() != null && o.getChamadoDate().isAfter(agora.minusHours(24)))
                .map(o -> o.getCliente().getId())
                .filter(clienteId -> !clienteId.equals(c.getCliente().getId()))
                .distinct()
                .count();

        return pontos + (int) Math.min(45, clientesComOMesmoProblema * 15);
    }

    private PrioridadeChamado nivel(int pontos) {
        if (pontos >= 70) return PrioridadeChamado.ALTA;
        if (pontos >= 40) return PrioridadeChamado.MEDIA;
        return PrioridadeChamado.BAIXA;
    }
}