package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.enums.PagamentoMetodo;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.dto.AgendamentoResponse;
import com.kaikeMartins.barberapp_backend.dto.CheckoutRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @InjectMocks
    private CheckoutService service;

    @Mock
    private AgendamentoService agendamentoService;

    @Mock
    private PagamentoService pagamentoService;

    private AgendamentoRequest agendamentoRequest;
    private AgendamentoResponse agendamentoResponse;

    @BeforeEach
    void setUp() {
        agendamentoRequest = new AgendamentoRequest(2L, 3L, LocalDate.now().plusDays(1), LocalTime.of(14, 0));
        agendamentoResponse = new AgendamentoResponse(10L, "Kaike", "Joao", "Corte", 40.0,
                agendamentoRequest.data(), agendamentoRequest.horario(), "PENDENTE");
    }

    @Test
    @DisplayName("Deve criar o agendamento e escolher Pix sem confirmar a reserva")
    void finalizarComPix() {
        when(agendamentoService.create(agendamentoRequest)).thenReturn(agendamentoResponse);

        AgendamentoResponse resposta = service.finalizar(new CheckoutRequest(agendamentoRequest, PagamentoMetodo.PIX));

        assertSame(agendamentoResponse, resposta);
        verify(pagamentoService).escolherMetodo(10L, PagamentoMetodo.PIX);
        verify(pagamentoService, never()).confirmarReservaPresencial(10L);
    }

    @Test
    @DisplayName("Deve criar o agendamento, escolher pagamento presencial e confirmar a reserva")
    void finalizarPresencial() {
        when(agendamentoService.create(agendamentoRequest)).thenReturn(agendamentoResponse);

        AgendamentoResponse resposta = service.finalizar(new CheckoutRequest(agendamentoRequest, PagamentoMetodo.PRESENCIAL));

        assertEquals(10L, resposta.id());
        verify(pagamentoService).escolherMetodo(10L, PagamentoMetodo.PRESENCIAL);
        verify(pagamentoService).confirmarReservaPresencial(10L);
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando o método é nulo sem criar agendamento")
    void finalizarSemMetodo() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.finalizar(new CheckoutRequest(agendamentoRequest, null)));

        assertEquals("Método de pagamento inválido.", e.getMessage());
        verifyNoInteractions(agendamentoService, pagamentoService);
    }
}
