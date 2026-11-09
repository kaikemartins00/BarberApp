package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.*;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.*;
import com.kaikeMartins.barberapp_backend.event.AgendamentosCanceladosEvent;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import com.kaikeMartins.barberapp_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BarbeiroServiceTest {

    @InjectMocks
    private BarbeiroService barbeiroService;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private UsuarioEntity usuarioBarbeiro;

    private UsuarioEntity usuarioCliente;

    private BarbeiroEntity barbeiro;

    private ClienteEntity cliente;

    private ServicoEntity corte;

    private AgendamentoEntity agendamento;

    private AgendamentoEntity agendamentoPassado;

    private LocalDate amanha;

    @BeforeEach
    void setUp() {
        amanha = LocalDate.now().plusDays(1);

        usuarioBarbeiro = UsuarioEntity.builder().id(2L).email("joao@email.com").senha("123456").verificado(true).build();
        usuarioCliente = UsuarioEntity.builder().id(1L).email("kaike@email.com").senha("123456").verificado(true).build();

        corte = ServicoEntity.builder().id(3L).nome("Corte").preco(40.0).build();

        barbeiro = BarbeiroEntity.builder()
                .id(2L)
                .usuario(usuarioBarbeiro)
                .nome("Joao")
                .telefone("79988888888")
                .servicos(new HashSet<>())
                .horariosTrabalho(new HashSet<>())
                .build();

        cliente = ClienteEntity.builder().id(1L).usuario(usuarioCliente).nome("Kaike").telefone("79999999999").build();

        agendamento = AgendamentoEntity.builder()
                .id(1L)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(corte)
                .data(amanha)
                .horario(LocalTime.of(14, 0))
                .status(AgendamentoStatus.PENDENTE)
                .build();

        agendamentoPassado = AgendamentoEntity.builder()
                .id(2L)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(corte)
                .data(LocalDate.now().minusDays(1))
                .horario(LocalTime.of(14, 0))
                .status(AgendamentoStatus.CONFIRMADO)
                .build();
    }

    @Test
    @DisplayName("Deve retornar todos os barbeiros com a agenda ordenada")
    void findAllSuccess() {
        LocalDateTime tarde = LocalDateTime.of(amanha, LocalTime.of(15, 0));
        LocalDateTime manha = LocalDateTime.of(amanha, LocalTime.of(9, 0));
        barbeiro.getHorariosTrabalho().add(tarde);
        barbeiro.getHorariosTrabalho().add(manha);
        barbeiro.getServicos().add(corte);
        when(barbeiroRepository.findAll()).thenReturn(List.of(barbeiro));

        List<BarbeiroResponse> lista = barbeiroService.findAll();

        assertEquals(1, lista.size());
        assertEquals("Joao", lista.get(0).nome());
        assertEquals(List.of(manha, tarde), lista.get(0).horarios());
        assertEquals(1, lista.get(0).servicos().size());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há barbeiros")
    void findAllEmptySuccess() {
        when(barbeiroRepository.findAll()).thenReturn(List.of());

        List<BarbeiroResponse> lista = barbeiroService.findAll();

        assertTrue(lista.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar o barbeiro pelo ID")
    void findByIdSuccess() {
        barbeiro.setFotoUrl("http://foto.com/j.jpg");
        barbeiro.getServicos().add(corte);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        BarbeiroResponse response = barbeiroService.findById(2L);

        assertEquals(2L, response.id());
        assertEquals("Joao", response.nome());
        assertEquals("http://foto.com/j.jpg", response.fotoUrl());
        assertTrue(response.servicos().contains(corte));
        verify(barbeiroRepository).findById(2L);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o barbeiro não existir")
    void findByIdNotFoundError() {
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> barbeiroService.findById(99L));

        assertThat(e.getMessage(), is("Barbeiro não encontrado"));
    }

    @Test
    @DisplayName("Deve atualizar o e-mail, a senha codificada e a foto")
    void updateSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(userRepository.existsByEmail("novo@email.com")).thenReturn(false);
        when(passwordEncoder.encode("novaSenha")).thenReturn("hash");
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        BarbeiroResponse response = barbeiroService.update(2L,
                new BarbeiroUpdateRequest("novo@email.com", "novaSenha", "http://foto.com/n.jpg"));

        assertEquals("novo@email.com", usuarioBarbeiro.getEmail());
        assertEquals("hash", usuarioBarbeiro.getSenha());
        assertEquals("http://foto.com/n.jpg", response.fotoUrl());
        verify(userRepository).existsByEmail("novo@email.com");
        verify(userRepository).save(usuarioBarbeiro);
        verify(barbeiroRepository).save(barbeiro);
    }

    @Test
    @DisplayName("Deve manter a senha atual quando a nova senha estiver em branco")
    void updateBlankPasswordSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(userRepository.existsByEmail("novo@email.com")).thenReturn(false);
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        barbeiroService.update(2L, new BarbeiroUpdateRequest("novo@email.com", "  ", null));

        assertEquals("123456", usuarioBarbeiro.getSenha());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Deve manter a senha atual quando a nova senha for nula")
    void updateNullPasswordSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(userRepository.existsByEmail("novo@email.com")).thenReturn(false);
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        barbeiroService.update(2L, new BarbeiroUpdateRequest("novo@email.com", null, "http://foto.com/n.jpg"));

        assertEquals("123456", usuarioBarbeiro.getSenha());
        assertEquals("http://foto.com/n.jpg", barbeiro.getFotoUrl());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Não deve verificar e-mail duplicado quando o e-mail for o mesmo, ignorando maiúsculas")
    void updateSameEmailSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        barbeiroService.update(2L, new BarbeiroUpdateRequest("JOAO@EMAIL.COM", null, null));

        assertEquals("JOAO@EMAIL.COM", usuarioBarbeiro.getEmail());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(usuarioBarbeiro);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o e-mail pertencer a outro usuário")
    void updateDuplicatedEmailError() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(userRepository.existsByEmail("outro@email.com")).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> barbeiroService.update(2L, new BarbeiroUpdateRequest("outro@email.com", null, null)));

        assertThat(e.getMessage(), is("O email já existe."));
        assertEquals("joao@email.com", usuarioBarbeiro.getEmail());
        verify(userRepository, never()).save(any());
        verify(barbeiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao atualizar um barbeiro inexistente")
    void updateNotFoundError() {
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> barbeiroService.update(99L, new BarbeiroUpdateRequest("a@email.com", null, null)));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Deve substituir a agenda do barbeiro")
    void atualizarHorariosSuccess() {
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(8, 0)));
        LocalDateTime h1 = LocalDateTime.of(amanha, LocalTime.of(10, 0));
        LocalDateTime h2 = LocalDateTime.of(amanha, LocalTime.of(9, 0));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        BarbeiroResponse response = barbeiroService.atualizarHorarios(2L, new HorariosRequest(List.of(h1, h2)));

        assertEquals(List.of(h2, h1), response.horarios());
        assertEquals(2, barbeiro.getHorariosTrabalho().size());
        verify(barbeiroRepository).save(barbeiro);
    }

    @Test
    @DisplayName("Deve esvaziar a agenda quando a lista de horários for vazia")
    void atualizarHorariosEmptySuccess() {
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(8, 0)));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        BarbeiroResponse response = barbeiroService.atualizarHorarios(2L, new HorariosRequest(List.of()));

        assertTrue(response.horarios().isEmpty());
        assertTrue(barbeiro.getHorariosTrabalho().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao atualizar horários de um barbeiro inexistente")
    void atualizarHorariosNotFoundError() {
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> barbeiroService.atualizarHorarios(99L, new HorariosRequest(List.of())));

        verify(barbeiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve substituir os serviços do barbeiro")
    void atualizarServicosSuccess() {
        ServicoEntity barba = ServicoEntity.builder().id(4L).nome("Barba").preco(30.0).build();
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findAllById(List.of(3L, 4L))).thenReturn(List.of(corte, barba));
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        BarbeiroResponse response = barbeiroService.atualizarServicos(2L, new ServicosIdsRequest(List.of(3L, 4L)));

        assertEquals(2, response.servicos().size());
        assertTrue(barbeiro.getServicos().containsAll(Set.of(corte, barba)));
        verify(barbeiroRepository).save(barbeiro);
    }

    @Test
    @DisplayName("Deve remover todos os serviços quando a lista de ids for vazia")
    void atualizarServicosEmptySuccess() {
        barbeiro.getServicos().add(corte);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(servicoRepository.findAllById(List.of())).thenReturn(List.of());
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        BarbeiroResponse response = barbeiroService.atualizarServicos(2L, new ServicosIdsRequest(List.of()));

        assertTrue(response.servicos().isEmpty());
        assertTrue(barbeiro.getServicos().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao atualizar serviços de um barbeiro inexistente")
    void atualizarServicosNotFoundError() {
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> barbeiroService.atualizarServicos(99L, new ServicosIdsRequest(List.of(3L))));

        verifyNoInteractions(servicoRepository);
        verify(barbeiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao cancelar o dia de um barbeiro inexistente")
    void cancelarDiaNotFoundError() {
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> barbeiroService.cancelarDia(99L, new CancelarDiaRequest(amanha, null, null)));

        verifyNoInteractions(agendamentoRepository, eventPublisher);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando a data final for anterior à data inicial")
    void cancelarDiaInvalidDatesError() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> barbeiroService.cancelarDia(2L, new CancelarDiaRequest(amanha, amanha.minusDays(1), null)));

        assertThat(e.getMessage(), is("A data final não pode ser anterior à data inicial."));
        verifyNoInteractions(agendamentoRepository, eventPublisher);
    }

    @Test
    @DisplayName("Deve cancelar apenas os agendamentos futuros, remover a agenda e publicar o evento")
    void cancelarDiaSuccess() {
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(9, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(14, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha.plusDays(5), LocalTime.of(9, 0)));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusIn(
                2L, amanha, amanha, List.of(AgendamentoStatus.PENDENTE, AgendamentoStatus.CONFIRMADO)))
                .thenReturn(List.of(agendamento, agendamentoPassado));

        CancelamentoResponse response = barbeiroService.cancelarDia(2L, new CancelarDiaRequest(amanha, null, "Doente"));

        assertEquals(1, response.agendamentosCancelados());
        assertEquals(2, response.horariosRemovidos());
        assertEquals(AgendamentoStatus.CANCELADO, agendamento.getStatus());
        assertEquals(AgendamentoStatus.CONFIRMADO, agendamentoPassado.getStatus());
        assertEquals(1, barbeiro.getHorariosTrabalho().size());
        verify(agendamentoRepository).saveAll(List.of(agendamento));
        verify(barbeiroRepository).save(barbeiro);
        verify(eventPublisher).publishEvent(any(AgendamentosCanceladosEvent.class));
    }

    @Test
    @DisplayName("Deve publicar o evento com os dados do aviso de cancelamento")
    void cancelarDiaEventContentSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusIn(eq(2L), eq(amanha), eq(amanha), any()))
                .thenReturn(List.of(agendamento));

        barbeiroService.cancelarDia(2L, new CancelarDiaRequest(amanha, null, "Doente"));

        ArgumentCaptor<AgendamentosCanceladosEvent> captor = ArgumentCaptor.forClass(AgendamentosCanceladosEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var aviso = captor.getValue().avisos().get(0);

        assertEquals(1, captor.getValue().avisos().size());
        assertEquals("kaike@email.com", aviso.email());
        assertEquals("Kaike", aviso.clienteNome());
        assertEquals("Joao", aviso.barbeiroNome());
        assertEquals("Corte", aviso.servicoNome());
        assertEquals(amanha, aviso.data());
        assertEquals(LocalTime.of(14, 0), aviso.horario());
        assertEquals("Doente", aviso.motivo());
    }

    @Test
    @DisplayName("Deve usar a data inicial como final quando ateData for nula")
    void cancelarDiaWithoutEndDateSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusIn(
                2L, amanha, amanha, List.of(AgendamentoStatus.PENDENTE, AgendamentoStatus.CONFIRMADO)))
                .thenReturn(List.of());

        CancelamentoResponse response = barbeiroService.cancelarDia(2L, new CancelarDiaRequest(amanha, null, null));

        assertEquals(0, response.agendamentosCancelados());
        verify(agendamentoRepository).findByBarbeiroIdAndDataBetweenAndStatusIn(
                2L, amanha, amanha, List.of(AgendamentoStatus.PENDENTE, AgendamentoStatus.CONFIRMADO));
    }

    @Test
    @DisplayName("Não deve cancelar nem publicar evento quando só há agendamentos passados")
    void cancelarDiaOnlyPastAppointmentsSuccess() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusIn(eq(2L), eq(amanha), eq(amanha), any()))
                .thenReturn(List.of(agendamentoPassado));

        CancelamentoResponse response = barbeiroService.cancelarDia(2L, new CancelarDiaRequest(amanha, null, null));

        assertEquals(0, response.agendamentosCancelados());
        assertEquals(AgendamentoStatus.CONFIRMADO, agendamentoPassado.getStatus());
        verify(agendamentoRepository).saveAll(List.of());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Deve cancelar um período e não publicar o evento quando não houver agendamentos")
    void cancelarPeriodoWithoutAppointmentsSuccess() {
        LocalDate fim = amanha.plusDays(2);
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(9, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(fim, LocalTime.of(9, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(fim.plusDays(1), LocalTime.of(9, 0)));
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusIn(eq(2L), eq(amanha), eq(fim), any()))
                .thenReturn(List.of());

        CancelamentoResponse response = barbeiroService.cancelarDia(2L, new CancelarDiaRequest(amanha, fim, null));

        assertEquals(0, response.agendamentosCancelados());
        assertEquals(2, response.horariosRemovidos());
        assertEquals(1, barbeiro.getHorariosTrabalho().size());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Deve excluir o barbeiro e o usuário")
    void deleteSuccess() {
        when(barbeiroRepository.existsById(2L)).thenReturn(true);

        barbeiroService.delete(2L);

        verify(barbeiroRepository).deleteById(2L);
        verify(userRepository).deleteById(2L);
        verify(userRepository).flush();
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao excluir um barbeiro inexistente")
    void deleteNotFoundError() {
        when(barbeiroRepository.existsById(99L)).thenReturn(false);

        NotFoundException e = assertThrows(NotFoundException.class, () -> barbeiroService.delete(99L));

        assertThat(e.getMessage(), is("Barbeiro não encontrado"));
        verify(barbeiroRepository, never()).deleteById(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o barbeiro possuir registros vinculados")
    void deleteLinkedRecordsError() {
        when(barbeiroRepository.existsById(2L)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("fk")).when(barbeiroRepository).deleteById(2L);

        BadRequestException e = assertThrows(BadRequestException.class, () -> barbeiroService.delete(2L));

        assertThat(e.getMessage(), is("Não dá para excluir: o barbeiro tem agendamentos ou chamados."));
        verify(barbeiroRepository).deleteById(2L);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando a exclusão do usuário violar integridade")
    void deleteUserLinkedRecordsError() {
        when(barbeiroRepository.existsById(2L)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("fk")).when(userRepository).deleteById(2L);

        BadRequestException e = assertThrows(BadRequestException.class, () -> barbeiroService.delete(2L));

        assertThat(e.getMessage(), is("Não dá para excluir: o barbeiro tem agendamentos ou chamados."));
        verify(userRepository, never()).flush();
    }
}