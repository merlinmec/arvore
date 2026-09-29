package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.ProgressoDTO;
import dev.joaomanoel.arvore.entity.Conclusao;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.util.Consistencia;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProgressoService {

    private final ConclusaoRepository conclusaoRepository;

    public ProgressoService(ConclusaoRepository conclusaoRepository) {
        this.conclusaoRepository = conclusaoRepository;
    }

    /**
     * Conclusões da semana atual (segunda a domingo) carregadas numa query só, pra calcular o
     * progresso de quantos conjuntos de rotinas a tela precisar.
     */
    public Semana semanaAtual() {
        LocalDate inicio = Datas.inicioDaSemana();
        LocalDate fim = Datas.fimDaSemana();
        Map<UUID, Set<LocalDate>> concluidas =
                conclusaoRepository.findAllByDataBetween(inicio, fim).stream()
                        .collect(
                                Collectors.groupingBy(
                                        Conclusao::getAtividadeId,
                                        Collectors.mapping(Conclusao::getData, Collectors.toSet())));
        return new Semana(inicio, fim, concluidas);
    }

    public record Semana(LocalDate inicio, LocalDate fim, Map<UUID, Set<LocalDate>> concluidas) {

        public ProgressoDTO progresso(Collection<Rotina> rotinas) {
            return Consistencia.progresso(rotinas, concluidas, inicio, fim);
        }
    }
}
