package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.RoleEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.RoleTypeEnum;
import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.RoleRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final RoleRepository roleRepository;

    public BarbeiroResponse toResponse(BarbeiroEntity entity) {
        return new BarbeiroResponse(
                entity.getId(),
                entity.getNome(),
                entity.getFotoUrl(),
                entity.getServicos(),
                entity.getHorariosTrabalho()
        );
    }

    public List<BarbeiroResponse> findAll() {
        return barbeiroRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BarbeiroResponse findById(Long id) {
        return barbeiroRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Barbeiro não encontrado"));
    }

    public BarbeiroEntity update(Long id, BarbeiroUpdateRequest request) {
        try {
            BarbeiroEntity entity = barbeiroRepository.getReferenceById(id);

            updateData(entity, request);

            return barbeiroRepository.save(entity);

        } catch (EntityNotFoundException e) {
            throw new NotFoundException("Cannot update. Barbeiro not found with id: " + id);
        }
    }

    private void updateData(BarbeiroEntity entity, BarbeiroUpdateRequest request) {
        entity.setEmail(request.email());
        entity.setSenha(request.senha());
        entity.setFotoUrl(request.fotoUrl());
    }

    private BarbeiroEntity barbeiroAutenticado() {
        return (BarbeiroEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public void atualizarHorarios(HorariosRequest request) {
        BarbeiroEntity barbeiro = barbeiroAutenticado();
        barbeiro.setHorariosTrabalho(new HashSet<>(request.horarios()));
        barbeiroRepository.save(barbeiro);
    }

    public void atualizarServicos(ServicosIdsRequest request) {
        BarbeiroEntity barbeiro = barbeiroAutenticado();
        Set<ServicoEntity> servicos = new HashSet<>(servicoRepository.findAllById(request.servicoIds()));
        barbeiro.setServicos(servicos);
        barbeiroRepository.save(barbeiro);
    }

    public void delete(Long id) {
        try {
            barbeiroRepository.deleteById(id);

        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Cannot delete. Barbeiro not found with id: " + id);

        } catch (Exception e) {
            throw new RuntimeException("Database error while deleting barber");
        }
    }
}
