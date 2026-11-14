package com.kaikeMartins.barberapp_backend.service;

import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.dto.ClienteBloqueadoResponse;
import com.kaikeMartins.barberapp_backend.event.ClienteDesbloqueadoEvent;
import com.kaikeMartins.barberapp_backend.exception.NotFoundException;
import com.kaikeMartins.barberapp_backend.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.kaikeMartins.barberapp_backend.support.Fixtures.cliente;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BloqueioServiceTest {

    @InjectMocks
    private BloqueioService service;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ClienteEntity bloqueadoAte(Long id, LocalDateTime ate) {
        ClienteEntity cliente = cliente(id);
        cliente.setBloqueadoAte(ate);
        return cliente;
    }

    @Test
    @DisplayName("Deve listar apenas clientes com bloqueio ativo, do que expira primeiro ao último")
    void listarBloqueados() {
        ClienteEntity expiraDepois = bloqueadoAte(1L, LocalDateTime.now().plusDays(5));
        ClienteEntity expiraAntes = bloqueadoAte(2L, LocalDateTime.now().plusDays(1));
        ClienteEntity jaExpirado = bloqueadoAte(3L, LocalDateTime.now().minusDays(1));
        ClienteEntity livre = bloqueadoAte(4L, null);
        when(clienteRepository.findAll()).thenReturn(List.of(expiraDepois, jaExpirado, livre, expiraAntes));

        List<ClienteBloqueadoResponse> lista = service.listarBloqueados();

        assertEquals(2, lista.size());
        assertEquals(2L, lista.get(0).id());
        assertEquals(1L, lista.get(1).id());
        assertEquals("usuario2@email.com", lista.get(0).email());
    }

    @Test
    @DisplayName("Deve devolver lista vazia quando ninguém está bloqueado")
    void listarBloqueadosVazio() {
        when(clienteRepository.findAll()).thenReturn(List.of(bloqueadoAte(1L, null)));

        assertTrue(service.listarBloqueados().isEmpty());
    }

    @Test
    @DisplayName("Deve desbloquear o cliente, registrar a data e avisar por e-mail")
    void desbloquearSuccess() {
        ClienteEntity cliente = bloqueadoAte(1L, LocalDateTime.now().plusDays(3));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        service.desbloquear(1L);

        assertNull(cliente.getBloqueadoAte());
        assertNotNull(cliente.getDesbloqueadoEm());
        verify(clienteRepository).save(cliente);
        verify(eventPublisher).publishEvent(any(ClienteDesbloqueadoEvent.class));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao desbloquear cliente inexistente")
    void desbloquearNaoEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException e = assertThrows(NotFoundException.class, () -> service.desbloquear(99L));

        assertEquals("Cliente não encontrado.", e.getMessage());
        verify(clienteRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ClienteDesbloqueadoEvent.class));
    }
}
