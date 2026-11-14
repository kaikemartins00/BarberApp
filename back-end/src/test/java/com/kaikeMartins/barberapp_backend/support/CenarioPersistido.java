package com.kaikeMartins.barberapp_backend.support;

import com.kaikeMartins.barberapp_backend.domain.entities.AgendamentoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ClienteEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.UsuarioEntity;
import com.kaikeMartins.barberapp_backend.domain.enums.AgendamentoStatus;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalTime;

public class CenarioPersistido {

    private final TestEntityManager em;

    public CenarioPersistido(TestEntityManager em) {
        this.em = em;
    }

    private UsuarioEntity usuario(String email) {
        return em.persist(UsuarioEntity.builder()
                .email(email)
                .senha("123456")
                .verificado(true)
                .build());
    }

    public ClienteEntity cliente(String email) {
        return em.persist(ClienteEntity.builder()
                .usuario(usuario(email))
                .nome("Cliente " + email)
                .telefone("79999999999")
                .build());
    }

    public BarbeiroEntity barbeiro(String email) {
        return em.persist(BarbeiroEntity.builder()
                .usuario(usuario(email))
                .nome("Barbeiro " + email)
                .telefone("79988888888")
                .build());
    }

    public ServicoEntity servico(String nome, double preco) {
        return em.persist(ServicoEntity.builder()
                .nome(nome)
                .preco(preco)
                .build());
    }

    public AgendamentoEntity agendamento(ClienteEntity cliente, BarbeiroEntity barbeiro, ServicoEntity servico,
                                         LocalDate data, LocalTime horario, AgendamentoStatus status) {
        return em.persist(AgendamentoEntity.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .data(data)
                .horario(horario)
                .status(status)
                .build());
    }
}
