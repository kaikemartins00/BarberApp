package com.kaikeMartins.barberapp_backend.ai;

import com.kaikeMartins.barberapp_backend.dto.AgendamentoRequest;
import com.kaikeMartins.barberapp_backend.exception.BadRequestException;
import com.kaikeMartins.barberapp_backend.repository.BarbeiroRepository;
import com.kaikeMartins.barberapp_backend.repository.ServicoRepository;
import com.kaikeMartins.barberapp_backend.service.AgendamentoService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AssistantTools {

    private static final Map<String, Double> SERVICE_PRICES = Map.of(
            "corte", 40.0,
            "barba", 30.0,
            "corte e barba", 65.0
    );

    private final BarbeiroRepository barbeiroRepository;
    private final AgendamentoService agendamentoService;
    private final ServicoRepository servicoRepository;

    @Tool("Retorna o preço de um serviço da barbearia (corte, barba ou corte e barba)")
    public String getServicePrice(String service) {
        Double price = SERVICE_PRICES.get(service.trim().toLowerCase());

        if (price == null) {
            return "Serviço não encontrado. Serviços disponíveis: " + SERVICE_PRICES.keySet();
        }
        return String.format("%s custa R$ %.2f", service, price);
    }

    @Tool("Agenda um serviço com um barbeiro. Use SOMENTE depois de o cliente confirmar serviço, barbeiro, data e horário. Data no formato yyyy-MM-dd e hora no formato HH:mm.")
    public String bookAppointment(@ToolMemoryId Long userId, String serviceName, String barberName, String date, String time) {
        try {
            AgendamentoRequest request = new AgendamentoRequest(
                    findBarberId(barberName),
                    findServiceId(serviceName),
                    LocalDate.parse(date),
                    LocalTime.parse(time));

            agendamentoService.create(request);
            return "Agendamento confirmado.";
        } catch (DateTimeParseException e) {
            return "Data ou hora em formato inválido. Use yyyy-MM-dd e HH:mm.";
        } catch (BadRequestException e) {
            return "Não foi possível agendar: " + e.getMessage();
        }
    }

    private Long findBarberId(String name) {
        return barbeiroRepository.findByNomeIgnoreCase(name.trim())
                .orElseThrow(() -> new BadRequestException("Barbeiro não encontrado: " + name))
                .getId();
    }

    private Long findServiceId(String name) {
        return servicoRepository.findByNomeIgnoreCase(name.trim())
                .orElseThrow(() -> new BadRequestException("Serviço não encontrado: " + name))
                .getId();
    }
}
