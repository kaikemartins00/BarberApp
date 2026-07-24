package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.dto.BarbeiroRequest;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.dto.HorariosRequest;
import com.kaikeMartins.barberapp_backend.dto.ServicosIdsRequest;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
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

    private final BarbeiroRepository repository;
    private final ServicoRepository servicoRepository;


    public List<BarbeiroEntity> findAll() {
        return repository.findAll();
    }

    public BarbeiroEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Barbeiro não encontrado"));
    }

    public BarbeiroEntity create(BarbeiroRequest request) {
        BarbeiroEntity entity = BarbeiroEntity.builder()
                .nome(request.nome())
                .fotoUrl(request.fotoUrl())
                .build();

        return repository.save(entity);
    }

    public BarbeiroEntity update(Long id, BarbeiroRequest request) {
        try {
            BarbeiroEntity entity = repository.getReferenceById(id);

            updateData(entity, request);

            return repository.save(entity);

        } catch (EntityNotFoundException e) {
            throw new NotFoundException("Cannot update. Barbeiro not found with id: " + id);
        }
    }

    private void updateData(BarbeiroEntity entity, BarbeiroRequest request) {
        entity.setNome(request.nome());
        entity.setFotoUrl(request.fotoUrl());
    }

    private BarbeiroEntity barbeiroAutenticado() {
        return (BarbeiroEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public void atualizarHorarios(HorariosRequest request) {
        BarbeiroEntity barbeiro = barbeiroAutenticado();
        barbeiro.setHorariosTrabalho(new HashSet<>(request.horarios()));
        repository.save(barbeiro);
    }

    public void atualizarServicos(ServicosIdsRequest request) {
        BarbeiroEntity barbeiro = barbeiroAutenticado();
        Set<ServicoEntity> servicos = new HashSet<>(servicoRepository.findAllById(request.servicoIds()));
        barbeiro.setServicos(servicos);
        repository.save(barbeiro);
    }

    public void delete(Long id) {
        try {
            repository.deleteById(id);

        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Cannot delete. Barbeiro not found with id: " + id);

        } catch (Exception e) {
            throw new RuntimeException("Database error while deleting barber");
        }
    }
}
