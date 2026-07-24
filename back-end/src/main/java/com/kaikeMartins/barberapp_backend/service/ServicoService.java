package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.dto.ServicoRequest;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository repository;

    public List<ServicoEntity> findAll() {
        return repository.findAll();
    }

    public ServicoEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com id: " + id));
    }

    public ServicoEntity create(ServicoRequest request) {
        ServicoEntity entity = ServicoEntity.builder()
                .nome(request.nome())
                .preco(request.preco())
                .build();

        return repository.save(entity);
    }

    public ServicoEntity update(Long id, ServicoRequest request) {
        try {
            ServicoEntity entity = repository.getReferenceById(id);
            updateData(entity, request);
            return repository.save(entity);

        } catch (EntityNotFoundException e) {
            throw new NotFoundException("Cannot update. Serviço not found with id: " + id);
        }
    }

    private void updateData(ServicoEntity entity, ServicoRequest request) {
        entity.setNome(request.nome());
        entity.setPreco(request.preco());
    }

    public void delete(Long id) {
        try {
            repository.deleteById(id);

        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Cannot delete. Serviço not found with id: " + id);

        } catch (Exception e) {
            throw new RuntimeException("Database error while deleting serviço");
        }
    }
}
