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
import org.mockito.ArgumentCaptor;
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

    private BarbeiroEntity outroBarbeiro;

    @BeforeEach
    void setUp() {
        corte = ServicoEntity.builder().id(1L).nome("Corte").preco(40.0).build();
        barba = ServicoEntity.builder().id(2L).nome("Barba").preco(30.0).build();

        UsuarioEntity usuario = UsuarioEntity.builder().id(10L).email("joao@email.com").senha("123456").verificado(true).build();
        UsuarioEntity outroUsuario = UsuarioEntity.builder().id(11L).email("pedro@email.com").senha("123456").verificado(true).build();

        barbeiro = BarbeiroEntity.builder()
                .id(10L)
                .usuario(usuario)
                .nome("Joao")
                .telefone("79988888888")
                .servicos(new HashSet<>(Set.of(corte, barba)))
                .horariosTrabalho(new HashSet<>())
                .build();

        outroBarbeiro = BarbeiroEntity.builder()
                .id(11L)
                .usuario(outroUsuario)
                .nome("Pedro")
                .telefone("79966666666")
                .servicos(new HashSet<>(Set.of(corte)))
                .horariosTrabalho(new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Deve retornar todos os serviços")
    void findAllSuccess() {
        when(repository.findAll()).thenReturn(List.of(corte, barba));

        List<ServicoEntity> lista = servicoService.findAll();

        assertEquals(2, lista.size());
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há serviços")
    void findAllEmptySuccess() {
        when(repository.findAll()).thenReturn(List.of());

        List<ServicoEntity> lista = servicoService.findAll();

        assertTrue(lista.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar o serviço pelo id")
    void findByIdSuccess() {
        when(repository.findById(1L)).thenReturn(Optional.of(corte));

        ServicoEntity servico = servicoService.findById(1L);

        assertSame(corte, servico);
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o serviço não existir")
    void findByIdNotFoundError() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> servicoService.findById(99L));

        assertThat(e.getMessage(), is("Serviço não encontrado com id: 99"));
    }

    @Test
    @DisplayName("Deve criar o serviço com o nome sem espaços nas pontas")
    void createSuccess() {
        when(repository.existsByNomeIgnoreCase("Sobrancelha")).thenReturn(false);
        when(repository.save(any(ServicoEntity.class))).thenAnswer(i -> i.getArgument(0));

        ServicoEntity salvo = servicoService.create(new ServicoRequest("  Sobrancelha  ", 15.5));

        ArgumentCaptor<ServicoEntity> captor = ArgumentCaptor.forClass(ServicoEntity.class);
        verify(repository).save(captor.capture());

        assertEquals("Sobrancelha", captor.getValue().getNome());
        assertEquals(15.5, captor.getValue().getPreco());
        assertEquals("Sobrancelha", salvo.getNome());
        assertEquals(15.5, salvo.getPreco());
    }

    @Test
    @DisplayName("Deve validar o nome já sem os espaços nas pontas ao criar")
    void createValidatesTrimmedNameSuccess() {
        when(repository.existsByNomeIgnoreCase("Sobrancelha")).thenReturn(false);
        when(repository.save(any(ServicoEntity.class))).thenAnswer(i -> i.getArgument(0));

        servicoService.create(new ServicoRequest("  Sobrancelha  ", 15.5));

        verify(repository).existsByNomeIgnoreCase("Sobrancelha");
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o nome do serviço já existir")
    void createDuplicatedNameError() {
        when(repository.existsByNomeIgnoreCase("corte")).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> servicoService.create(new ServicoRequest("corte", 40.0)));

        assertThat(e.getMessage(), is("Já existe um serviço com esse nome."));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar o nome e o preço do serviço")
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
    @DisplayName("Deve atualizar apenas o preço mantendo o nome")
    void updateOnlyPriceSuccess() {
        when(repository.getReferenceById(1L)).thenReturn(corte);
        when(repository.save(any(ServicoEntity.class))).thenReturn(corte);

        ServicoEntity atualizado = servicoService.update(1L, new ServicoRequest("Corte", 55.0));

        assertEquals("Corte", atualizado.getNome());
        assertEquals(55.0, atualizado.getPreco());
        verify(repository, never()).existsByNomeIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Não deve validar nome duplicado quando o nome é o mesmo ignorando maiúsculas")
    void updateSameNameSuccess() {
        when(repository.getReferenceById(1L)).thenReturn(corte);
        when(repository.save(any(ServicoEntity.class))).thenReturn(corte);

        ServicoEntity atualizado = servicoService.update(1L, new ServicoRequest("CORTE", 45.0));

        assertEquals("CORTE", atualizado.getNome());
        assertEquals(45.0, atualizado.getPreco());
        verify(repository, never()).existsByNomeIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Não deve validar nome duplicado quando o nome é o mesmo apesar dos espaços nas pontas")
    void updateSameNameWithSpacesSuccess() {
        when(repository.getReferenceById(1L)).thenReturn(corte);
        when(repository.save(any(ServicoEntity.class))).thenReturn(corte);

        ServicoEntity atualizado = servicoService.update(1L, new ServicoRequest("  corte  ", 45.0));

        assertEquals("corte", atualizado.getNome());
        verify(repository, never()).existsByNomeIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o novo nome já existir na atualização")
    void updateDuplicatedNameError() {
        when(repository.getReferenceById(1L)).thenReturn(corte);
        when(repository.existsByNomeIgnoreCase("Barba")).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> servicoService.update(1L, new ServicoRequest("Barba", 30.0)));

        assertThat(e.getMessage(), is("Já existe um serviço com esse nome."));
        assertEquals("Corte", corte.getNome());
        assertEquals(40.0, corte.getPreco());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao atualizar um serviço inexistente")
    void updateNotFoundError() {
        when(repository.getReferenceById(99L)).thenThrow(new EntityNotFoundException("nao existe"));

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> servicoService.update(99L, new ServicoRequest("X", 10.0)));

        assertThat(e.getMessage(), is("Não foi possível atualizar. Serviço não encontrado com id: 99"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve remover o serviço dos barbeiros antes de excluí-lo")
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
    @DisplayName("Deve remover o serviço de todos os barbeiros que o oferecem")
    void deleteMultipleBarbersSuccess() {
        when(repository.existsById(1L)).thenReturn(true);
        when(agendamentoRepository.existsByServicoId(1L)).thenReturn(false);
        when(barbeiroRepository.findByServicosId(1L)).thenReturn(List.of(barbeiro, outroBarbeiro));

        servicoService.delete(1L);

        assertFalse(barbeiro.getServicos().contains(corte));
        assertTrue(outroBarbeiro.getServicos().isEmpty());
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("Deve excluir o serviço quando nenhum barbeiro o oferece")
    void deleteWithoutBarbersSuccess() {
        when(repository.existsById(1L)).thenReturn(true);
        when(agendamentoRepository.existsByServicoId(1L)).thenReturn(false);
        when(barbeiroRepository.findByServicosId(1L)).thenReturn(List.of());

        servicoService.delete(1L);

        verify(barbeiroRepository).flush();
        verify(repository).deleteById(1L);
        verify(repository).flush();
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao excluir um serviço inexistente")
    void deleteNotFoundError() {
        when(repository.existsById(99L)).thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class, () -> servicoService.delete(99L));

        assertThat(e.getMessage(), is("Serviço não encontrado com id: 99"));
        verify(repository, never()).deleteById(any());
        verifyNoInteractions(agendamentoRepository, barbeiroRepository);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando existem agendamentos com o serviço")
    void deleteWithAppointmentsError() {
        when(repository.existsById(1L)).thenReturn(true);
        when(agendamentoRepository.existsByServicoId(1L)).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class, () -> servicoService.delete(1L));

        assertThat(e.getMessage(), is("Não é possível excluir: já existem agendamentos com esse serviço."));
        verify(repository, never()).deleteById(any());
        verifyNoInteractions(barbeiroRepository);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o serviço ainda estiver em uso ao excluir")
    void deleteStillInUseError() {
        when(repository.existsById(1L)).thenReturn(true);
        when(agendamentoRepository.existsByServicoId(1L)).thenReturn(false);
        when(barbeiroRepository.findByServicosId(1L)).thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("fk")).when(repository).deleteById(1L);

        BadRequestException e = assertThrows(BadRequestException.class, () -> servicoService.delete(1L));

        assertThat(e.getMessage(), is("Não é possível excluir: o serviço ainda está em uso."));
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o flush da exclusão violar integridade")
    void deleteFlushIntegrityError() {
        when(repository.existsById(1L)).thenReturn(true);
        when(agendamentoRepository.existsByServicoId(1L)).thenReturn(false);
        when(barbeiroRepository.findByServicosId(1L)).thenReturn(List.of());
        doThrow(new DataIntegrityViolationException("fk")).when(repository).flush();

        BadRequestException e = assertThrows(BadRequestException.class, () -> servicoService.delete(1L));

        assertThat(e.getMessage(), is("Não é possível excluir: o serviço ainda está em uso."));
        verify(repository).deleteById(1L);
    }
}