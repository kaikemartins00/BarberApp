package com.kaikeMartins.barberapp_backend.ai;

import com.kaikeMartins.barberapp_backend.domain.entities.BarbeiroEntity;
import com.kaikeMartins.barberapp_backend.domain.entities.ServicoEntity;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import com.kaikeMartins.barberapp_backend.service.AgendamentoService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ferramentas de LEITURA do assistente: tudo vem do banco (preços e barbeiros reais).
 * O assistente não agenda: o agendamento é feito pelos botões do chat ou pela tela de agendamento.
 */
@Component
@RequiredArgsConstructor
public class AssistantTools {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final AgendamentoService agendamentoService;

    private String preco(ServicoEntity s) {
        return String.format("R$ %.2f", s.getPreco());
    }

    @Tool("Lista todos os serviços da barbearia com os preços atuais")
    @Transactional(readOnly = true)
    public String listarServicos() {
        List<ServicoEntity> servicos = servicoRepository.findAll();
        if (servicos.isEmpty()) return "Ainda não há serviços cadastrados.";
        return servicos.stream()
                .sorted(Comparator.comparing(ServicoEntity::getNome))
                .map(s -> s.getNome() + ": " + preco(s))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Retorna o preço atual de um serviço pelo nome")
    @Transactional(readOnly = true)
    public String getServicePrice(String service) {
        if (service == null || service.isBlank()) {
            return "Informe o nome do serviço.\n" + listarServicos();
        }
        return servicoRepository.findByNomeIgnoreCase(service.trim())
                .map(s -> s.getNome() + " custa " + preco(s))
                .orElse("Serviço não encontrado. Serviços disponíveis:\n" + listarServicos());
    }

    @Tool("Lista os barbeiros da barbearia e os serviços (com preço) que cada um faz")
    @Transactional(readOnly = true)
    public String listarBarbeiros() {
        List<BarbeiroEntity> barbeiros = barbeiroRepository.findAll().stream()
                .filter(b -> b.getDesligadoEm() == null).toList();
        if (barbeiros.isEmpty()) return "Ainda não há barbeiros cadastrados.";
        return barbeiros.stream()
                .sorted(Comparator.comparing(BarbeiroEntity::getNome))
                .map(b -> b.getNome() + ": " + (b.getServicos().isEmpty() ? "sem serviços cadastrados" :
                        b.getServicos().stream()
                                .sorted(Comparator.comparing(ServicoEntity::getNome))
                                .map(s -> s.getNome() + " (" + preco(s) + ")")
                                .collect(Collectors.joining(", "))))
                .collect(Collectors.joining("\n"));
    }

    @Tool("Lista os horários livres de um barbeiro em uma data. Data no formato yyyy-MM-dd.")
    @Transactional(readOnly = true)
    public String horariosLivres(String barberName, String date) {
        try {
            BarbeiroEntity barbeiro = barbeiroRepository.findByNomeIgnoreCase(barberName.trim())
                    .orElseThrow(() -> new BadRequestException("Barbeiro não encontrado: " + barberName));
            LocalDate data = LocalDate.parse(date);
            LocalTime agora = LocalTime.now();
            List<String> livres = agendamentoService.horariosLivres(barbeiro.getId(), data).stream()
                    .filter(h -> !data.isEqual(LocalDate.now()) || h.isAfter(agora))
                    .map(h -> h.format(HORA))
                    .toList();
            return livres.isEmpty()
                    ? barbeiro.getNome() + " não tem horários livres em " + data + "."
                    : barbeiro.getNome() + " tem livre em " + data + ": " + String.join(", ", livres);
        } catch (DateTimeParseException e) {
            return "Data em formato inválido. Use yyyy-MM-dd.";
        } catch (BadRequestException e) {
            return e.getMessage();
        }
    }
}
