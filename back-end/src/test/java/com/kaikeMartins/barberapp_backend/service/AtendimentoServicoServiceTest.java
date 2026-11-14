package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.PagamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoStatus;
import com.kaikeMartins.barberapp_backend.dto.AdicionarServicoAtendimentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AlterarValorServicoRequest;
import com.kaikeMartins.barberapp_backend.dto.ContaAtendimentoResponse;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.exception.ForbiddenException;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.AgendamentoServicoRepository;
import com.kaikeMartins.barberapp_backend.repository.PagamentoRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import com.kaikeMartins.barberapp_backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtendimentoServicoServiceTest {

    @InjectMocks
    private AtendimentoServicoService service;

    @Mock
    private AgendamentoServicoRepository itemRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private ServicoRepository servicoRepository;

    @Mock
    private UserRepository userRepository;

    private ClienteEntity cliente;
    private BarbeiroEntity barbeiro;
    private ServicoEntity corte;
    private ServicoEntity barba;
    private AgendamentoEntity agendamento;
    private PagamentoEntity pagamento;
    private AgendamentoServicoEntity itemOriginal;

    @BeforeEach
    void setUp() {
        cliente = cliente(1L);
        barbeiro = barbeiro(2L);
        corte = servico(3L, 40.0);
        barba = servico(4L, 25.0);
        barbeiro.getServicos().add(corte);
        barbeiro.getServicos().add(barba);
        agendamento = agendamento(10L, cliente, barbeiro, corte, LocalDate.now().plusDays(1),
                LocalTime.of(14, 0), AgendamentoStatus.CONFIRMADO);
        pagamento = PagamentoEntity.builder()
                .id(100L)
                .agendamento(agendamento)
                .valor(new BigDecimal("40.00"))
                .metodo(PagamentoMetodo.PRESENCIAL)
                .status(PagamentoStatus.PENDENTE)
                .build();
        itemOriginal = AgendamentoServicoEntity.builder()
                .id(1L)
                .agendamento(agendamento)
                .servico(corte)
                .valorCobrado(new BigDecimal("40.00"))
                .extra(false)
                .build();
        autenticar(barbeiro.getUsuario());
    }

    @AfterEach
    void tearDown() {
        limparAutenticacao();
    }

    private AgendamentoServicoEntity extra(Long id, ServicoEntity servico, String valor) {
        return AgendamentoServicoEntity.builder()
                .id(id)
                .agendamento(agendamento)
                .servico(servico)
                .valorCobrado(new BigDecimal(valor))
                .extra(true)
                .build();
    }

    private void pagamentoBloqueavel() {
        when(pagamentoRepository.buscarParaAtualizacao(10L)).thenReturn(Optional.of(pagamento));
    }

    @Test
    @DisplayName("Deve registrar o serviço inicial com o preço do catálogo")
    void registrarServicoInicial() {
        when(itemRepository.findByAgendamentoIdOrderByIdAsc(10L)).thenReturn(List.of());

        service.registrarServicoInicial(agendamento);

        ArgumentCaptor<AgendamentoServicoEntity> captor = ArgumentCaptor.forClass(AgendamentoServicoEntity.class);
        verify(itemRepository).save(captor.capture());
        assertEquals(new BigDecimal("40.00"), captor.getValue().getValorCobrado());
        assertFalse(captor.getValue().isExtra());
        assertSame(corte, captor.getValue().getServico());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao registrar serviço inicial duas vezes")
    void registrarServicoInicialDuplicado() {
        when(itemRepository.findByAgendamentoIdOrderByIdAsc(10L)).thenReturn(List.of(itemOriginal));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.registrarServicoInicial(agendamento));

        assertEquals("Os serviços deste atendimento já foram registrados.", e.getMessage());
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve consultar a conta quando o usuário é o cliente do atendimento")
    void consultarComoCliente() {
        autenticar(cliente.getUsuario());
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(pagamentoRepository.findByAgendamentoId(10L)).thenReturn(Optional.of(pagamento));
        when(itemRepository.findByAgendamentoIdOrderByIdAsc(10L)).thenReturn(List.of(itemOriginal));

        ContaAtendimentoResponse conta = service.consultar(10L);

        assertEquals(1, conta.servicos().size());
        assertEquals(new BigDecimal("40.00"), conta.valorTotal());
        assertEquals("PENDENTE", conta.pagamentoStatus());
        assertFalse(conta.servicos().get(0).extra());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando um estranho consulta a conta")
    void consultarEstranho() {
        autenticar(usuario(9L, CLIENTE));
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        ForbiddenException e = assertThrows(ForbiddenException.class, () -> service.consultar(10L));

        assertEquals("Você não tem acesso a esse atendimento.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o agendamento da conta não existe")
    void consultarAgendamentoInexistente() {
        when(agendamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.consultar(99L));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o pagamento da conta não existe")
    void consultarSemPagamento() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));
        when(pagamentoRepository.findByAgendamentoId(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.consultar(10L));
    }

    @Test
    @DisplayName("Deve adicionar um serviço extra e atualizar o total do pagamento")
    void adicionarExtra() {
        pagamentoBloqueavel();
        when(servicoRepository.findById(4L)).thenReturn(Optional.of(barba));
        when(itemRepository.findByAgendamentoIdOrderByIdAsc(10L))
                .thenReturn(List.of(itemOriginal, extra(2L, barba, "25.00")));

        ContaAtendimentoResponse conta = service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, null));

        assertEquals(new BigDecimal("65.00"), pagamento.getValor());
        assertEquals(2, conta.servicos().size());
        ArgumentCaptor<AgendamentoServicoEntity> captor = ArgumentCaptor.forClass(AgendamentoServicoEntity.class);
        verify(itemRepository).saveAndFlush(captor.capture());
        assertTrue(captor.getValue().isExtra());
        assertEquals(new BigDecimal("25.00"), captor.getValue().getValorCobrado());
        verify(pagamentoRepository).save(pagamento);
    }

    @Test
    @DisplayName("Deve adicionar o extra com o valor combinado e voltar o pagamento para pendente")
    void adicionarExtraComValorCombinado() {
        pagamento.setStatus(PagamentoStatus.AGUARDANDO_CONFIRMACAO);
        pagamentoBloqueavel();
        when(servicoRepository.findById(4L)).thenReturn(Optional.of(barba));
        when(itemRepository.findByAgendamentoIdOrderByIdAsc(10L))
                .thenReturn(List.of(itemOriginal, extra(2L, barba, "10.50")));

        service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, new BigDecimal("10.50")));

        assertEquals(new BigDecimal("50.50"), pagamento.getValor());
        assertEquals(PagamentoStatus.PENDENTE, pagamento.getStatus());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o barbeiro não realiza o serviço extra")
    void adicionarServicoQueBarbeiroNaoFaz() {
        barbeiro.getServicos().remove(barba);
        pagamentoBloqueavel();
        when(servicoRepository.findById(4L)).thenReturn(Optional.of(barba));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, null)));

        assertEquals("Esse barbeiro não realiza esse serviço.", e.getMessage());
        verify(itemRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o valor do extra tem mais de duas casas decimais")
    void adicionarValorInvalido() {
        pagamentoBloqueavel();
        when(servicoRepository.findById(4L)).thenReturn(Optional.of(barba));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, new BigDecimal("10.555"))));

        assertEquals("Informe um valor válido, maior que zero e com até duas casas decimais.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o serviço extra não existe")
    void adicionarServicoInexistente() {
        pagamentoBloqueavel();
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.adicionar(10L, new AdicionarServicoAtendimentoRequest(99L, null)));
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando o cliente tenta alterar a conta")
    void adicionarComoCliente() {
        autenticar(cliente.getUsuario());
        pagamentoBloqueavel();

        ForbiddenException e = assertThrows(ForbiddenException.class,
                () -> service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, null)));

        assertEquals("Somente o barbeiro responsável ou administrador pode alterar a conta.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException quando outro barbeiro tenta alterar a conta")
    void adicionarComoOutroBarbeiro() {
        autenticar(usuario(7L, BARBEIRO));
        pagamentoBloqueavel();

        assertThrows(ForbiddenException.class,
                () -> service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, null)));
    }

    @Test
    @DisplayName("Deve permitir que o administrador altere a conta")
    void adicionarComoAdmin() {
        autenticar(usuario(5L, ADMIN));
        pagamentoBloqueavel();
        when(servicoRepository.findById(4L)).thenReturn(Optional.of(barba));
        when(itemRepository.findByAgendamentoIdOrderByIdAsc(10L))
                .thenReturn(List.of(itemOriginal, extra(2L, barba, "25.00")));

        ContaAtendimentoResponse conta = service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, null));

        assertEquals(new BigDecimal("65.00"), conta.valorTotal());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o pagamento já foi pago")
    void adicionarPagamentoPago() {
        pagamento.setStatus(PagamentoStatus.PAGO);
        pagamentoBloqueavel();

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, null)));

        assertEquals("Esse pagamento não permite alterações.", e.getMessage());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o atendimento foi cancelado")
    void adicionarAtendimentoCancelado() {
        agendamento.setStatus(AgendamentoStatus.CANCELADO);
        pagamentoBloqueavel();

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.adicionar(10L, new AdicionarServicoAtendimentoRequest(4L, null)));

        assertEquals("Não é possível alterar a conta desse atendimento.", e.getMessage());
    }

    @Test
    @DisplayName("Deve alterar o valor cobrado de um item e recalcular o total")
    void alterarValor() {
        pagamentoBloqueavel();
        when(itemRepository.findByIdAndAgendamentoId(1L, 10L)).thenReturn(Optional.of(itemOriginal));
        when(itemRepository.findByAgendamentoIdOrderByIdAsc(10L)).thenReturn(List.of(itemOriginal));

        ContaAtendimentoResponse conta = service.alterarValor(10L, 1L, new AlterarValorServicoRequest(new BigDecimal("35.00")));

        assertEquals(new BigDecimal("35.00"), itemOriginal.getValorCobrado());
        assertEquals(new BigDecimal("35.00"), conta.valorTotal());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao alterar item que não pertence ao atendimento")
    void alterarValorItemInexistente() {
        pagamentoBloqueavel();
        when(itemRepository.findByIdAndAgendamentoId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.alterarValor(10L, 99L, new AlterarValorServicoRequest(new BigDecimal("35.00"))));
    }

    @Test
    @DisplayName("Deve remover um serviço extra e recalcular o total")
    void removerExtra() {
        AgendamentoServicoEntity extra = extra(2L, barba, "25.00");
        pagamento.setValor(new BigDecimal("65.00"));
        pagamentoBloqueavel();
        when(itemRepository.findByIdAndAgendamentoId(2L, 10L)).thenReturn(Optional.of(extra));
        when(itemRepository.findByAgendamentoIdOrderByIdAsc(10L)).thenReturn(List.of(itemOriginal));

        ContaAtendimentoResponse conta = service.removerExtra(10L, 2L);

        verify(itemRepository).delete(extra);
        assertEquals(new BigDecimal("40.00"), pagamento.getValor());
        assertEquals(1, conta.servicos().size());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException ao remover o serviço original do agendamento")
    void removerServicoOriginal() {
        pagamentoBloqueavel();
        when(itemRepository.findByIdAndAgendamentoId(1L, 10L)).thenReturn(Optional.of(itemOriginal));

        BadRequestException e = assertThrows(BadRequestException.class, () -> service.removerExtra(10L, 1L));

        assertEquals("O serviço original do agendamento não pode ser removido.", e.getMessage());
        verify(itemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando o pagamento para edição não existe")
    void removerSemPagamento() {
        when(pagamentoRepository.buscarParaAtualizacao(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.removerExtra(10L, 2L));
    }
}
