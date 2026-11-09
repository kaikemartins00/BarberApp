package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.dto.ServicoRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository repository;
    private final BarbeiroRepository barbeiroRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public List<ServicoEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public ServicoEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Serviço não encontrado com id: " + id));
    }

    @Transactional
    public ServicoEntity create(ServicoRequest request) {
        String nome = request.nome().trim();
        validarNomeUnico(nome);

        return repository.save(ServicoEntity.builder()
                .nome(nome)
                .preco(request.preco())
                .build());
    }

    @Transactional
    public ServicoEntity update(Long id, ServicoRequest request) {
        try {
            ServicoEntity entity = repository.getReferenceById(id);

            String novoNome = request.nome().trim();
            if (!entity.getNome().equalsIgnoreCase(novoNome)) {
                validarNomeUnico(novoNome);
            }

            updateData(entity, request);
            return repository.save(entity);

        } catch (EntityNotFoundException e) {
            throw new NotFoundException("Não foi possível atualizar. Serviço não encontrado com id: " + id);
        }
    }

    private void updateData(ServicoEntity entity, ServicoRequest request) {
        entity.setNome(request.nome().trim());
        entity.setPreco(request.preco());
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Serviço não encontrado com id: " + id);
        }

        if (agendamentoRepository.existsByServicoId(id)) {
            throw new BadRequestException(
                    "Não é possível excluir: já existem agendamentos com esse serviço.");
        }

        barbeiroRepository.findByServicosId(id)
                .forEach(b -> b.getServicos().removeIf(s -> s.getId().equals(id)));
        barbeiroRepository.flush();

        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Não é possível excluir: o serviço ainda está em uso.");
        }
    }

    private void validarNomeUnico(String nome) {
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new BadRequestException("Já existe um serviço com esse nome.");
        }
    }
}