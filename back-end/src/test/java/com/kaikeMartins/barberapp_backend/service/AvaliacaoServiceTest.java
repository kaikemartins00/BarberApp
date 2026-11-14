package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.AvaliacaoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AvaliacaoRequest;
import com.kaikeMartins.barberapp_backend.dto.AvaliacaoResponse;
import com.kaikeMartins.barberapp_backend.dto.AvaliacaoResumoResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.AvaliacaoRepository;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTest {

    @InjectMocks
    private AvaliacaoService service;

    @Mock
    private AvaliacaoRepository repository;

    @Mock
    private AgendamentoRepository agendamentos;

    @Mock
    private BarbeiroRepository barbeiros;

    private ClienteEntity cliente;
    private BarbeiroEntity barbeiro;
    private AgendamentoEntity agendamento;

    @BeforeEach
    void setUp() {
        cliente = cliente(1L);
        barbeiro = barbeiro(2L);
        agendamento = agendamento(10L, cliente, barbeiro, servico(3L, 40.0),
                LocalDate.now().minusDays(1), LocalTime.of(14, 0), AgendamentoStatus.CONCLUIDO);
        autenticar(cliente.getUsuario());
    }

    @AfterEach
    void tearDown() {
        limparAutenticacao();
    }

    private AvaliacaoRequest request(int nota, List<String> tags) {
        return new AvaliacaoRequest(10L, nota, "Ótimo", tags);
    }

    private void agendamentoExiste() {
        when(agendamentos.findById(10L)).thenReturn(Optional.of(agendamento));
    }

    @Test
    @DisplayName("Deve criar a avaliação de um atendimento concluído com as características")
    void criarSuccess() {
        agendamentoExiste();
        when(repository.existsByAgendamentoId(10L)).thenReturn(false);
        when(repository.save(any(AvaliacaoEntity.class))).thenAnswer(i -> i.getArgument(0));

        AvaliacaoResponse resposta = service.criar(request(5, List.of("pontual", "atencioso")));

        assertEquals(5, resposta.nota());
        assertEquals(10L, resposta.agendamentoId());
        assertEquals(2L, resposta.barbeiroId());
        assertEquals(List.of("pontual", "atencioso"), resposta.tags());
    }

    @Test
    @DisplayName("Deve criar a avaliação sem características")
    void criarSemTags() {
        agendamentoExiste();
        when(repository.existsByAgendamentoId(10L)).thenReturn(false);
        when(repository.save(any(AvaliacaoEntity.class))).thenAnswer(i -> i.getArgument(0));

        AvaliacaoResponse resposta = service.criar(request(4, null));

        assertTrue(resposta.tags().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o agendamento não existe")
    void criarAgendamentoInexistente() {
        when(agendamentos.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.criar(request(5, null)));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o agendamento é de outro cliente")
    void criarDeOutroCliente() {
        autenticar(usuario(9L, CLIENTE));
        agendamentoExiste();

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.criar(request(5, null)));

        assertEquals("Agendamento não pertence ao cliente.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o atendimento não foi concluído")
    void criarNaoConcluido() {
        agendamento.setStatus(AgendamentoStatus.CONFIRMADO);
        agendamentoExiste();

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.criar(request(5, null)));

        assertEquals("Somente atendimentos concluídos podem ser avaliados.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando a nota é menor que 1")
    void criarNotaBaixa() {
        agendamentoExiste();

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.criar(request(0, null)));

        assertEquals("A nota deve estar entre 1 e 5.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando a nota é maior que 5")
    void criarNotaAlta() {
        agendamentoExiste();

        assertThrows(BadRequestException.class, () -> service.criar(request(6, null)));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o atendimento já foi avaliado")
    void criarJaAvaliado() {
        agendamentoExiste();
        when(repository.existsByAgendamentoId(10L)).thenReturn(true);

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.criar(request(5, null)));

        assertEquals("Esse atendimento já foi avaliado.", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando há mais de 5 características")
    void criarTagsDemais() {
        agendamentoExiste();
        when(repository.existsByAgendamentoId(10L)).thenReturn(false);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.criar(request(5, List.of("a", "b", "c", "d", "e", "f"))));

        assertEquals("Tags inválidas.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando uma característica tem vírgula")
    void criarTagComVirgula() {
        agendamentoExiste();
        when(repository.existsByAgendamentoId(10L)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.criar(request(5, List.of("legal,bom"))));
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando uma característica está em branco")
    void criarTagEmBranco() {
        agendamentoExiste();
        when(repository.existsByAgendamentoId(10L)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.criar(request(5, List.of(" "))));
    }

    @Test
    @DisplayName("Deve informar que o cliente dono já avaliou o atendimento")
    void porAgendamentoCliente() {
        agendamentoExiste();
        when(repository.existsByAgendamentoId(10L)).thenReturn(true);

        assertTrue(service.porAgendamento(10L));
    }

    @Test
    @DisplayName("Deve permitir que o administrador consulte se o atendimento foi avaliado")
    void porAgendamentoAdmin() {
        autenticar(usuario(5L, ADMIN));
        agendamentoExiste();
        when(repository.existsByAgendamentoId(10L)).thenReturn(false);

        assertFalse(service.porAgendamento(10L));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando um estranho consulta a avaliação")
    void porAgendamentoEstranho() {
        autenticar(usuario(9L, CLIENTE));
        agendamentoExiste();

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.porAgendamento(10L));

        assertEquals("Acesso negado.", e.getMessage());
    }

    @Test
    @DisplayName("Deve calcular a média e a quantidade de avaliações do barbeiro")
    void resumoSuccess() {
        when(barbeiros.existsById(2L)).thenReturn(true);
        when(repository.findByAgendamentoBarbeiroId(2L)).thenReturn(List.of(
                AvaliacaoEntity.builder().nota(5).criadaEm(LocalDateTime.now()).build(),
                AvaliacaoEntity.builder().nota(4).criadaEm(LocalDateTime.now()).build()));

        AvaliacaoResumoResponse resumo = service.resumo(2L);

        assertEquals(4.5, resumo.media());
        assertEquals(2, resumo.quantidade());
    }

    @Test
    @DisplayName("Deve devolver média zero quando o barbeiro não tem avaliações")
    void resumoSemAvaliacoes() {
        when(barbeiros.existsById(2L)).thenReturn(true);
        when(repository.findByAgendamentoBarbeiroId(2L)).thenReturn(List.of());

        AvaliacaoResumoResponse resumo = service.resumo(2L);

        assertEquals(0.0, resumo.media());
        assertEquals(0, resumo.quantidade());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao resumir avaliações de barbeiro inexistente")
    void resumoBarbeiroInexistente() {
        when(barbeiros.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.resumo(99L));
    }
}
