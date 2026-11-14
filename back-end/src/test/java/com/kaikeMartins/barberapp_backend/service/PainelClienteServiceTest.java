package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.AvaliacaoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.NotaAgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.PreferenciasClienteRequest;
import com.kaikeMartins.barberapp_backend.dto.PreferenciasClienteResponse;
import com.kaikeMartins.barberapp_backend.dto.ServicoPopularResponse;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.AvaliacaoRepository;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PainelClienteServiceTest {

    @InjectMocks
    private PainelClienteService service;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    private ClienteEntity cliente;

    @BeforeEach
    void setUp() {
        cliente = cliente(1L);
        autenticar(cliente.getUsuario());
    }

    @AfterEach
    void tearDown() {
        limparAutenticacao();
    }

    private void clienteExiste() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
    }

    @Test
    @DisplayName("Deve listar as notas que o cliente deu aos agendamentos")
    void notasDoCliente() {
        clienteExiste();
        AgendamentoEntity agendamento = agendamento(10L, cliente, barbeiro(2L), servico(3L, 40.0),
                LocalDate.now(), LocalTime.of(9, 0), AgendamentoStatus.CONCLUIDO);
        when(avaliacaoRepository.findByAgendamentoClienteId(1L))
                .thenReturn(List.of(AvaliacaoEntity.builder().agendamento(agendamento).nota(5).build()));

        List<NotaAgendamentoResponse> notas = service.notasDoCliente();

        assertEquals(1, notas.size());
        assertEquals(10L, notas.get(0).agendamentoId());
        assertEquals(5, notas.get(0).nota());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o usuário logado não é cliente")
    void notasDeQuemNaoECliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.notasDoCliente());

        assertEquals("Apenas clientes podem usar esta função.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o principal não é um usuário")
    void notasComPrincipalInvalido() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.notasDoCliente());

        assertEquals("Usuário não autenticado ou sessão inválida.", e.getMessage());
    }

    @Test
    @DisplayName("Deve devolver apenas os 3 serviços mais feitos")
    void servicosMaisFeitos() {
        when(agendamentoRepository.servicosMaisFeitos()).thenReturn(List.of(
                new Object[]{"Corte", 10L},
                new Object[]{"Barba", 7L},
                new Object[]{"Sobrancelha", 3},
                new Object[]{"Pigmentação", 1L}));

        List<ServicoPopularResponse> lista = service.servicosMaisFeitos();

        assertEquals(3, lista.size());
        assertEquals("Corte", lista.get(0).servico());
        assertEquals(10L, lista.get(0).quantidade());
        assertEquals(3L, lista.get(2).quantidade());
    }

    @Test
    @DisplayName("Deve devolver as preferências salvas do cliente")
    void preferencias() {
        cliente.setPreferencias("Corte baixo");
        clienteExiste();

        assertEquals("Corte baixo", service.preferencias().observacoes());
    }

    @Test
    @DisplayName("Deve devolver texto vazio quando o cliente não tem preferências")
    void preferenciasVazias() {
        clienteExiste();

        assertEquals("", service.preferencias().observacoes());
    }

    @Test
    @DisplayName("Deve salvar as preferências removendo espaços nas pontas")
    void salvarPreferencias() {
        clienteExiste();

        PreferenciasClienteResponse resposta = service.salvarPreferencias(new PreferenciasClienteRequest("  Máquina 2  "));

        assertEquals("Máquina 2", resposta.observacoes());
        assertEquals("Máquina 2", cliente.getPreferencias());
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve limpar as preferências quando o texto vem em branco")
    void salvarPreferenciasEmBranco() {
        cliente.setPreferencias("Antiga");
        clienteExiste();

        PreferenciasClienteResponse resposta = service.salvarPreferencias(new PreferenciasClienteRequest("   "));

        assertEquals("", resposta.observacoes());
        assertNull(cliente.getPreferencias());
    }

    @Test
    @DisplayName("Deve limpar as preferências quando o texto é nulo")
    void salvarPreferenciasNulas() {
        clienteExiste();

        PreferenciasClienteResponse resposta = service.salvarPreferencias(new PreferenciasClienteRequest(null));

        assertEquals("", resposta.observacoes());
        assertNull(cliente.getPreferencias());
    }
}
