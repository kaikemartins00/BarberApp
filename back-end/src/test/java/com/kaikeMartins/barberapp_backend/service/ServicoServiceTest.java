package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.dto.ServicoRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServicoServiceTest {

    @InjectMocks
    private ServicoService servicoService;

    @Mock
    private ServicoRepository repository;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    private ServicoEntity corte;

    private ServicoEntity barba;

    private BarbeiroEntity barbeiro;

    @BeforeEach
    void setUp() {
        corte = ServicoEntity.builder().id(1L).nome("Corte").preco(40.0).build();
        barba = ServicoEntity.builder().id(2L).nome("Barba").preco(30.0).build();

        UsuarioEntity usuario = UsuarioEntity.builder().id(10L).email("joao@email.com").senha("123456").verificado(true).build();
        barbeiro = BarbeiroEntity.builder().id(10L).usuario(usuario).nome("Joao").telefone("79988888888")
                .servicos(new HashSet<>(Set.of(corte, barba))).horariosTrabalho(new HashSet<>()).build();
    }

    @Test
    @DisplayName("Should return all services")
    void findAllSuccess() {

        when(repository.findAll()).thenReturn(List.of(corte, barba));

        List<ServicoEntity> lista = servicoService.findAll();

        assertEquals(2, lista.size());

        verify(repository).findAll();
    }

    @Test
    @DisplayName("Should return service by id")
    void findByIdSuccess() {

        when(repository.findById(1L)).thenReturn(Optional.of(corte));

        ServicoEntity servico = servicoService.findById(1L);

        assertSame(corte, servico);

        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when service does not exist")
    void findByIdNotFoundError() {

        when(repository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> servicoService.findById(99L));

        assertThat(e.getMessage(), is("Serviço não encontrado com id: 99"));
    }

    @Test
    @DisplayName("Should create service with trimmed name")
    void createSuccess() {

        when(repository.existsByNomeIgnoreCase("Sobrancelha")).thenReturn(false);
        when(repository.save(any(ServicoEntity.class))).thenAnswer(i -> i.getArgument(0));

        ServicoEntity salvo = servicoService.create(new ServicoRequest("  Sobrancelha  ", 15.5));

        assertEquals("Sobrancelha", salvo.getNome());
        assertEquals(15.5, salvo.getPreco());

        verify(repository).save(any(ServicoEntity.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when service name already exists")
    void createDuplicatedNameError() {

        when(repository.existsByNomeIgnoreCase("corte")).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> servicoService.create(new ServicoRequest("corte", 40.0)));

        assertThat(e.getMessage(), is("Já existe um serviço com esse nome."));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update service name and price")
    void updateSuccess() {

        when(repository.getReferenceById(1L)).thenReturn(corte);
        when(repository.existsByNomeIgnoreCase("Corte Premium")).thenReturn(false);
        when(repository.save(any(ServicoEntity.class))).thenReturn(corte);

        ServicoEntity atualizado = servicoService.update(1L, new ServicoRequest(" Corte Premium ", 60.0));

        assertEquals("Corte Premium", atualizado.getNome());
        assertEquals(60.0, atualizado.getPreco());

        verify(repository).getReferenceById(1L);
        verify(repository).save(corte);
    }

    @Test
    @DisplayName("Should not validate duplicated name when name is the same ignoring case")
    void updateSameNameSuccess() {

        when(repository.getReferenceById(1L)).thenReturn(corte);
        when(repository.save(any(ServicoEntity.class))).thenReturn(corte);

        ServicoEntity atualizado = servicoService.update(1L, new ServicoRequest("CORTE", 45.0));

        assertEquals("CORTE", atualizado.getNome());
        assertEquals(45.0, atualizado.getPreco());

        verify(repository, never()).existsByNomeIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Should throw BadRequestException when new name already exists on update")
    void updateDuplicatedNameError() {

        when(repository.getReferenceById(1L)).thenReturn(corte);
        when(repository.existsByNomeIgnoreCase("Barba")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> servicoService.update(1L, new ServicoRequest("Barba", 30.0)));

        assertEquals("Corte", corte.getNome());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating non-existent service")
    void updateNotFoundError() {

        when(repository.getReferenceById(99L)).thenThrow(new EntityNotFoundException("nao existe"));

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> servicoService.update(99L, new ServicoRequest("X", 10.0)));

        assertThat(e.getMessage(), is("Não foi possível atualizar. Serviço não encontrado com id: 99"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should remove service from barbers before deleting it")
    void deleteSuccess() {

        when(repository.existsById(1L)).thenReturn(true);
        when(agendamentoRepository.existsByServicoId(1L)).thenReturn(false);
        when(barbeiroRepository.findByServicosId(1L)).thenReturn(List.of(barbeiro));

        servicoService.delete(1L);

        assertEquals(1, barbeiro.getServicos().size());
        assertTrue(barbeiro.getServicos().contains(barba));

        verify(barbeiroRepository).flush();
        verify(repository).deleteById(1L);
        verify(repository).flush();
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existent service")
    void deleteNotFoundError() {

        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> servicoService.delete(99L));

        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when service has appointments")
    void deleteWithAppointmentsError() {

        when(repository.existsById(1L)).thenReturn(true);
        when(agendamentoRepository.existsByServicoId(1L)).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class, () -> servicoService.delete(1L));

        assertThat(e.getMessage(), is("Não é possível excluir: já existem agendamentos com esse serviço."));

        verify(repository, never()).deleteById(any());
        verifyNoInteractions(barbeiroRepository);
    }

    @Test
    @DisplayName("Should throw BadRequestException when service is still in use")
    void deleteStillInUseError() {

        when(repository.existsById(1L)).thenReturn(true);
        when(agendamentoRepository.existsByServicoId(1L)).thenReturn(false);
        when(barbeiroRepository.findByServicosId(1L)).thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("fk")).when(repository).deleteById(1L);

        BadRequestException e = assertThrows(BadRequestException.class, () -> servicoService.delete(1L));

        assertThat(e.getMessage(), is("Não é possível excluir: o serviço ainda está em uso."));

        verify(repository).deleteById(1L);
    }
}