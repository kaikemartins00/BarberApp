package com.kaikeMartins.barberapp_backend.controller;

import com.kaikeMartins.barberapp_backend.dto.ClienteBloqueadoResponse;
import com.kaikeMartins.barberapp_backend.service.BloqueioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BloqueioController {

    private final BloqueioService service;

    @GetMapping("/bloqueados")
    public List<ClienteBloqueadoResponse> bloqueados() {
        return service.listarBloqueados();
    }

    @PostMapping("/{id}/desbloquear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desbloquear(@PathVariable Long id) {
        service.desbloquear(id);
    }
}
