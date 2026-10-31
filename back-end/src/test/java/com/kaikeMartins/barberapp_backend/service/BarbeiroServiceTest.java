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

    private LocalDate amanha;

    @BeforeEach
    void setUp() {
        amanha = LocalDate.now().plusDays(1);

        usuarioBarbeiro = UsuarioEntity.builder().id(2L).email("joao@email.com").senha("123456").verificado(true).build();
        usuarioCliente = UsuarioEntity.builder().id(1L).email("kaike@email.com").senha("123456").verificado(true).build();

        corte = ServicoEntity.builder().id(3L).nome("Corte").preco(40.0).build();

        barbeiro = BarbeiroEntity.builder().id(2L).usuario(usuarioBarbeiro).nome("Joao").telefone("79988888888")
                .servicos(new HashSet<>()).horariosTrabalho(new HashSet<>()).build();
        cliente = ClienteEntity.builder().id(1L).usuario(usuarioCliente).nome("Kaike").telefone("79999999999").build();
    }

    private AgendamentoEntity agendamento(Long id, LocalDate data, AgendamentoStatus status) {
        return AgendamentoEntity.builder().id(id).cliente(cliente).barbeiro(barbeiro).servico(corte)
                .data(data).horario(LocalTime.of(14, 0)).status(status).build();
    }

    @Test
    @DisplayName("Should return all barbers with sorted schedule")
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
    @DisplayName("Should return barber by id")
    void findByIdSuccess() {

        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        BarbeiroResponse response = barbeiroService.findById(2L);

        assertEquals(2L, response.id());
        assertEquals("Joao", response.nome());

        verify(barbeiroRepository).findById(2L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when barber does not exist")
    void findByIdNotFoundError() {

        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> barbeiroService.findById(99L));

        assertThat(e.getMessage(), is("Barbeiro não encontrado"));
    }

    @Test
    @DisplayName("Should update email, encoded password and photo")
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

        verify(userRepository).save(usuarioBarbeiro);
        verify(barbeiroRepository).save(barbeiro);
    }

    @Test
    @DisplayName("Should keep current password when new password is blank")
    void updateBlankPasswordSuccess() {

        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(userRepository.existsByEmail("novo@email.com")).thenReturn(false);
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        barbeiroService.update(2L, new BarbeiroUpdateRequest("novo@email.com", "  ", null));

        assertEquals("123456", usuarioBarbeiro.getSenha());

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Should not check duplicated email when email is the same ignoring case")
    void updateSameEmailSuccess() {

        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(barbeiroRepository.save(any(BarbeiroEntity.class))).thenReturn(barbeiro);

        barbeiroService.update(2L, new BarbeiroUpdateRequest("JOAO@EMAIL.COM", null, null));

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when email belongs to another user")
    void updateDuplicatedEmailError() {

        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(userRepository.existsByEmail("outro@email.com")).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> barbeiroService.update(2L, new BarbeiroUpdateRequest("outro@email.com", null, null)));

        assertThat(e.getMessage(), is("O email já existe."));

        verify(userRepository, never()).save(any());
        verify(barbeiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating non-existent barber")
    void updateNotFoundError() {

        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> barbeiroService.update(99L, new BarbeiroUpdateRequest("a@email.com", null, null)));
    }

    @Test
    @DisplayName("Should replace barber schedule")
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
    @DisplayName("Should replace barber services")
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
    @DisplayName("Should throw BadRequestException when end date is before start date")
    void cancelarDiaInvalidDatesError() {

        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> barbeiroService.cancelarDia(2L, new CancelarDiaRequest(amanha, amanha.minusDays(1), null)));

        assertThat(e.getMessage(), is("A data final não pode ser anterior à data inicial."));

        verifyNoInteractions(agendamentoRepository, eventPublisher);
    }

    @Test
    @DisplayName("Should cancel only future appointments, remove schedule and publish event")
    void cancelarDiaSuccess() {

        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(9, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha, LocalTime.of(14, 0)));
        barbeiro.getHorariosTrabalho().add(LocalDateTime.of(amanha.plusDays(5), LocalTime.of(9, 0)));

        AgendamentoEntity futuro = agendamento(10L, amanha, AgendamentoStatus.PENDENTE);
        AgendamentoEntity passado = agendamento(11L, LocalDate.now().minusDays(1), AgendamentoStatus.CONFIRMADO);

        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusIn(
                2L, amanha, amanha, List.of(AgendamentoStatus.PENDENTE, AgendamentoStatus.CONFIRMADO)))
                .thenReturn(List.of(futuro, passado));

        CancelamentoResponse response = barbeiroService.cancelarDia(2L, new CancelarDiaRequest(amanha, null, "Doente"));

        assertEquals(1, response.agendamentosCancelados());
        assertEquals(2, response.horariosRemovidos());
        assertEquals(AgendamentoStatus.CANCELADO, futuro.getStatus());
        assertEquals(AgendamentoStatus.CONFIRMADO, passado.getStatus());
        assertEquals(1, barbeiro.getHorariosTrabalho().size());

        verify(agendamentoRepository).saveAll(List.of(futuro));
        verify(barbeiroRepository).save(barbeiro);
        verify(eventPublisher).publishEvent(any(AgendamentosCanceladosEvent.class));
    }

    @Test
    @DisplayName("Should cancel a period and not publish event when there are no appointments")
    void cancelarPeriodoNoAppointmentsSuccess() {

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

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should delete barber and user")
    void deleteSuccess() {

        when(barbeiroRepository.existsById(2L)).thenReturn(true);

        barbeiroService.delete(2L);

        verify(barbeiroRepository).deleteById(2L);
        verify(userRepository).deleteById(2L);
        verify(userRepository).flush();
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existent barber")
    void deleteNotFoundError() {

        when(barbeiroRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> barbeiroService.delete(99L));

        verify(barbeiroRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when barber has linked records")
    void deleteLinkedRecordsError() {

        when(barbeiroRepository.existsById(2L)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("fk")).when(barbeiroRepository).deleteById(2L);

        BadRequestException e = assertThrows(BadRequestException.class, () -> barbeiroService.delete(2L));

        assertThat(e.getMessage(), is("Não dá para excluir: o barbeiro tem agendamentos ou chamados."));

        verify(barbeiroRepository).deleteById(2L);
    }
}