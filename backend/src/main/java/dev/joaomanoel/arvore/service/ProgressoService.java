package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.ProgressoDTO;
import dev.joaomanoel.arvore.repository.AgendaSemanalRepository;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.util.Datas;
import dev.joaomanoel.arvore.util.ProgressoCalculo;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProgressoService {

    private final AgendaSemanalRepository agendaSemanalRepository;
    private final ConclusaoRepository conclusaoRepository;

    public ProgressoService(
            AgendaSemanalRepository agendaSemanalRepository, ConclusaoRepository conclusaoRepository) {
        this.agendaSemanalRepository = agendaSemanalRepository;
        this.conclusaoRepository = conclusaoRepository;
    }

    /** Progresso da semana atual: agendado (planejado) vs. concluído, pro conjunto de atividades dado. */
    public ProgressoDTO calcular(List<UUID> atividadeIds) {
        if (atividadeIds.isEmpty()) {
            return ProgressoCalculo.calcular(0, 0);
        }

        int planejado =
                atividadeIds.stream()
                        .mapToInt(id -> agendaSemanalRepository.findAllByAtividadeId(id).size())
                        .sum();

        LocalDate inicio = Datas.inicioDaSemana();
        LocalDate fim = Datas.fimDaSemana();
        int concluido =
                conclusaoRepository
                        .findAllByAtividadeIdInAndDataBetween(atividadeIds, inicio, fim)
                        .size();

        return ProgressoCalculo.calcular(planejado, concluido);
    }
}
