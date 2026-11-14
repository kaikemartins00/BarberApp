package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.OcupacaoResponse;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcupacaoServiceTest {

    @InjectMocks
    private OcupacaoService service;

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    private LocalDate dia;
    private BarbeiroEntity barbeiro;
    private ClienteEntity cliente;
    private ServicoEntity servico;

    @BeforeEach
    void setUp() {
        dia = LocalDate.now().plusDays(2);
        barbeiro = barbeiro(2L);
        cliente = cliente(1L);
        servico = servico(3L, 40.0);
        autenticar(barbeiro.getUsuario());
    }

    @AfterEach
    void tearDown() {
        limparAutenticacao();
    }

    private void abrirHorarios(int... horas) {
        for (int hora : horas) {
            barbeiro.getHorariosTrabalho().add(LocalDateTime.of(dia, LocalTime.of(hora, 0)));
        }
    }

    private AgendamentoEntity reserva(Long id, int hora, AgendamentoStatus status) {
        return agendamento(id, cliente, barbeiro, servico, dia, LocalTime.of(hora, 0), status);
    }

    @Test
    @DisplayName("Deve calcular a ocupação ignorando cancelados e horários fora da agenda")
    void consultarCalculaOcupacao() {
        abrirHorarios(9, 10, 11, 12);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndData(2L, dia)).thenReturn(List.of(
                reserva(1L, 9, AgendamentoStatus.CONFIRMADO),
                reserva(2L, 10, AgendamentoStatus.CANCELADO),
                reserva(3L, 18, AgendamentoStatus.CONFIRMADO)));

        OcupacaoResponse resposta = service.consultar(2L, dia);

        assertEquals(4, resposta.horariosDisponibilizados());
        assertEquals(1, resposta.horariosOcupados());
        assertEquals(3, resposta.horariosLivres());
        assertEquals(25.0, resposta.percentualOcupacao());
    }

    @Test
    @DisplayName("Deve devolver ocupação zerada quando o barbeiro não abriu horários no dia")
    void consultarSemHorarios() {
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndData(2L, dia)).thenReturn(List.of());

        OcupacaoResponse resposta = service.consultar(2L, dia);

        assertEquals(0, resposta.horariosDisponibilizados());
        assertEquals(0, resposta.horariosLivres());
        assertEquals(0.0, resposta.percentualOcupacao());
    }

    @Test
    @DisplayName("Deve permitir que o administrador consulte a ocupação de qualquer barbeiro")
    void consultarComoAdmin() {
        autenticar(usuario(5L, ADMIN));
        abrirHorarios(9);
        when(barbeiroRepository.findById(2L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndData(2L, dia)).thenReturn(List.of());

        assertEquals(1, service.consultar(2L, dia).horariosLivres());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando um barbeiro consulta a ocupação de outro")
    void consultarOutroBarbeiro() {
        autenticar(usuario(7L, BARBEIRO));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.consultar(2L, dia));

        assertEquals("Você não pode consultar a ocupação de outro barbeiro", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando não há usuário autenticado")
    void consultarSemAutenticacao() {
        limparAutenticacao();

        assertThrows(ForbiddenException.class, () -> service.consultar(2L, dia));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o principal não é um usuário")
    void consultarPrincipalInvalido() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThrows(ForbiddenException.class, () -> service.consultar(2L, dia));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o barbeiro não existe")
    void consultarBarbeiroInexistente() {
        autenticar(usuario(5L, ADMIN));
        when(barbeiroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.consultar(99L, dia));
    }
}
