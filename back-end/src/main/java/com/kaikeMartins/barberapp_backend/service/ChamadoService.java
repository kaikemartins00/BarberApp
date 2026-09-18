package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ChamadoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.RespostaChamadoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.ChamadoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PrioridadeChamado;
import com.kaikeMartins.barberapp_backend.dto.ChamadoRequest;
import com.kaikeMartins.barberapp_backend.dto.ChamadoResponse;
import com.kaikeMartins.barberapp_backend.dto.RespostaChamRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.exception.ResourceNotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ChamadoRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import com.kaikeMartins.barberapp_backend.repository.RespostaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final RespostaRepository respostaRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;

    public ChamadoResponse toResponse(ChamadoEntity entity) {
        return new ChamadoResponse(
                entity.getId(),
                entity.getTitulo(),
                entity.getDescricao(),
                entity.getChamadoDate(),
                entity.getStatus(),
                entity.getPrioridade(),
                entity.getCliente(),
                entity.getBarbeiro()
        );
    }

    public List<ChamadoResponse> findAll() {
        return chamadoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ChamadoEntity create(ChamadoRequest request) {
        ClienteEntity cliente = getClienteAutenticado();

        PrioridadeChamado prioridade = getPrioridade(request);

        ChamadoEntity entity = ChamadoEntity.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .cliente(cliente)
                .status(ChamadoStatus.ABERTO)
                .prioridade(prioridade)
                .build();
        
        return chamadoRepository.save(entity);
    }

    @Transactional
    public RespostaChamadoEntity iniciarChamado(Long id, RespostaChamRequest request) {
        ChamadoEntity chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado nao existe"));

        BarbeiroEntity barbeiro = getBarbeiroAutenticado();

        chamado.setBarbeiro(barbeiro);
        chamado.setStatus(ChamadoStatus.EM_ANDAMENTO);

        RespostaChamadoEntity respostaEntity = RespostaChamadoEntity.builder()
                .respostaMsg(request.respostaMsg())
                .barbeiro(barbeiro)
                .chamado(chamado)
                .build();


        return respostaRepository.save(respostaEntity);
    }

    @Transactional
    public void finalizarChamado(Long id) {
        ChamadoEntity chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado nao existe"));

        if (chamado.getStatus() == ChamadoStatus.FINALIZADO) {
            throw new BadRequestException("Esse chamado ja foi finalizado");
        }

        chamado.setStatus(ChamadoStatus.FINALIZADO);
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

    private ClienteEntity getClienteAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return clienteRepository.findByEmail(authentication.getName()).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private BarbeiroEntity getBarbeiroAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return barbeiroRepository.findByEmail(authentication.getName()).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
