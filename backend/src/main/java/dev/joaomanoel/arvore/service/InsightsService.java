package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.AreaConsistenciaDTO;
import dev.joaomanoel.arvore.dto.AtividadeConsistenciaDTO;
import dev.joaomanoel.arvore.dto.DiaConsistenciaDTO;
import dev.joaomanoel.arvore.dto.DiaSemanaConsistenciaDTO;
import dev.joaomanoel.arvore.dto.ResumoInsightsDTO;
import dev.joaomanoel.arvore.dto.SerieDiaDTO;
import dev.joaomanoel.arvore.dto.SerieDiaDTO.ContagemAreaDTO;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.Conclusao;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.util.Consistencia;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InsightsService {

    public static final int MAX_DIAS_RESUMO = 365;
    public static final int MAX_DIAS_HISTORICO = 400;
    private static final int JANELA_STREAK_DIAS = 180;

    private final CatalogoService catalogoService;
    private final ConclusaoRepository conclusaoRepository;

    public InsightsService(CatalogoService catalogoService, ConclusaoRepository conclusaoRepository) {
        this.catalogoService = catalogoService;
        this.conclusaoRepository = conclusaoRepository;
    }

    /** Planejado x concluído dia a dia, pra heatmap e tira semanal. */
    public List<DiaConsistenciaDTO> historico(LocalDate inicio, LocalDate fim, UUID areaId) {
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Início depois do fim");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAX_DIAS_HISTORICO) {
            throw new IllegalArgumentException("Período máximo é de " + MAX_DIAS_HISTORICO + " dias");
        }
        Catalogo catalogo = carregar(areaId);
        List<Atividade> atividades = catalogo.atividadesEmCurso(areaId);
        Map<UUID, Set<LocalDate>> concluidas = concluidasPorAtividade(atividades, inicio, fim);

        List<DiaConsistenciaDTO> dias = new ArrayList<>();
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            int planejado = 0;
            int concluido = 0;
            for (Atividade atividade : atividades) {
                if (catalogo.rotina(atividade.getId()).planejadaEm(data)) {
                    planejado++;
                    if (concluidas.getOrDefault(atividade.getId(), Set.of()).contains(data)) {
                        concluido++;
                    }
                }
            }
            dias.add(new DiaConsistenciaDTO(data, planejado, concluido));
        }
        return dias;
    }

    public ResumoInsightsDTO resumo(int dias, UUID areaId) {
        if (dias < 1 || dias > MAX_DIAS_RESUMO) {
            throw new IllegalArgumentException("Período deve ter entre 1 e " + MAX_DIAS_RESUMO + " dias");
        }
        LocalDate hoje = Datas.hoje();
        LocalDate inicio = hoje.minusDays(dias - 1L);
        LocalDate inicioAnterior = inicio.minusDays(dias);
        LocalDate limiteStreak = hoje.minusDays(JANELA_STREAK_DIAS);

        Catalogo catalogo = carregar(areaId);
        List<Atividade> atividades = catalogo.atividadesEmCurso(areaId);
        LocalDate inicioBusca = inicioAnterior.isBefore(limiteStreak) ? inicioAnterior : limiteStreak;
        Map<UUID, Set<LocalDate>> concluidas = concluidasPorAtividade(atividades, inicioBusca, hoje);

        Acumulador periodo = new Acumulador();
        Acumulador anterior = new Acumulador();
        Map<LocalDate, Map<UUID, int[]>> porDataERamo = new LinkedHashMap<>();
        for (LocalDate data = inicio; !data.isAfter(hoje); data = data.plusDays(1)) {
            porDataERamo.put(data, new LinkedHashMap<>());
        }
        Map<DiaSemana, int[]> porDiaSemana = new EnumMap<>(DiaSemana.class);
        for (DiaSemana dia : DiaSemana.values()) {
            porDiaSemana.put(dia, new int[2]);
        }
        Map<UUID, int[]> porRamo = new LinkedHashMap<>();
        List<AtividadeConsistenciaDTO> porAtividade = new ArrayList<>();

        for (Atividade atividade : atividades) {
            Rotina rotina = catalogo.rotina(atividade.getId());
            Set<LocalDate> feitas = concluidas.getOrDefault(atividade.getId(), Set.of());
            Area area = catalogo.areaDe(atividade);
            UUID ramo = catalogo.ramoDe(area.getId(), areaId).getId();

            int planejadoAtividade = 0;
            int concluidoAtividade = 0;
            for (LocalDate data = inicio; !data.isAfter(hoje); data = data.plusDays(1)) {
                if (!rotina.planejadaEm(data)) {
                    continue;
                }
                boolean feita = feitas.contains(data);
                int[] celula = porDataERamo.get(data).computeIfAbsent(ramo, chave -> new int[2]);
                int[] diaSemana = porDiaSemana.get(Datas.diaSemanaDe(data));
                int[] doRamo = porRamo.computeIfAbsent(ramo, chave -> new int[2]);
                celula[0]++;
                diaSemana[0]++;
                doRamo[0]++;
                planejadoAtividade++;
                if (feita) {
                    celula[1]++;
                    diaSemana[1]++;
                    doRamo[1]++;
                    concluidoAtividade++;
                }
            }
            periodo.somar(planejadoAtividade, concluidoAtividade);

            for (LocalDate data = inicioAnterior; data.isBefore(inicio); data = data.plusDays(1)) {
                if (rotina.planejadaEm(data)) {
                    anterior.somar(1, feitas.contains(data) ? 1 : 0);
                }
            }

            porAtividade.add(
                    new AtividadeConsistenciaDTO(
                            atividade.getId(),
                            atividade.getNome(),
                            catalogo.objetivoDe(atividade).getDescricao(),
                            area.getId(),
                            area.getNome(),
                            area.getCorTema(),
                            planejadoAtividade,
                            concluidoAtividade,
                            Consistencia.taxa(planejadoAtividade, concluidoAtividade),
                            Consistencia.sequenciaAtual(rotina, feitas, hoje, limiteStreak),
                            Consistencia.maiorSequencia(rotina, feitas, inicio, hoje)));
        }

        List<SerieDiaDTO> serie = new ArrayList<>();
        int diasComPlano = 0;
        int diasPerfeitos = 0;
        for (var entrada : porDataERamo.entrySet()) {
            int planejado = entrada.getValue().values().stream().mapToInt(celula -> celula[0]).sum();
            int concluido = entrada.getValue().values().stream().mapToInt(celula -> celula[1]).sum();
            if (planejado > 0) {
                diasComPlano++;
                if (concluido == planejado) {
                    diasPerfeitos++;
                }
            }
            List<ContagemAreaDTO> contagens =
                    entrada.getValue().entrySet().stream()
                            .filter(celula -> celula.getValue()[1] > 0)
                            .map(celula -> new ContagemAreaDTO(celula.getKey(), celula.getValue()[1]))
                            .toList();
            serie.add(new SerieDiaDTO(entrada.getKey(), planejado, concluido, contagens));
        }

        List<AreaConsistenciaDTO> porArea =
                porRamo.entrySet().stream()
                        .map(
                                entrada -> {
                                    Area area = catalogo.area(entrada.getKey());
                                    int[] valores = entrada.getValue();
                                    return new AreaConsistenciaDTO(
                                            area.getId(),
                                            area.getNome(),
                                            area.getCorTema(),
                                            valores[0],
                                            valores[1],
                                            Consistencia.taxa(valores[0], valores[1]));
                                })
                        .sorted(Comparator.comparingInt(AreaConsistenciaDTO::planejado).reversed())
                        .toList();

        List<DiaSemanaConsistenciaDTO> semana =
                porDiaSemana.entrySet().stream()
                        .map(
                                entrada ->
                                        new DiaSemanaConsistenciaDTO(
                                                entrada.getKey(),
                                                entrada.getValue()[0],
                                                entrada.getValue()[1],
                                                Consistencia.taxa(entrada.getValue()[0], entrada.getValue()[1])))
                        .toList();

        List<AtividadeConsistenciaDTO> atividadesOrdenadas =
                porAtividade.stream()
                        .sorted(
                                Comparator.comparing(
                                                AtividadeConsistenciaDTO::taxa,
                                                Comparator.nullsLast(Comparator.reverseOrder()))
                                        .thenComparing(
                                                AtividadeConsistenciaDTO::concluido, Comparator.reverseOrder()))
                        .toList();

        return new ResumoInsightsDTO(
                inicio,
                hoje,
                periodo.planejado,
                periodo.concluido,
                Consistencia.taxa(periodo.planejado, periodo.concluido),
                Consistencia.taxa(anterior.planejado, anterior.concluido),
                diasComPlano,
                diasPerfeitos,
                serie,
                porArea,
                semana,
                atividadesOrdenadas);
    }

    private Catalogo carregar(UUID areaId) {
        Catalogo catalogo = catalogoService.carregar();
        if (areaId != null && !catalogo.existeArea(areaId)) {
            throw new NaoEncontradoException("Área não encontrada");
        }
        return catalogo;
    }

    private Map<UUID, Set<LocalDate>> concluidasPorAtividade(
            List<Atividade> atividades, LocalDate inicio, LocalDate fim) {
        if (atividades.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = atividades.stream().map(Atividade::getId).toList();
        return conclusaoRepository.findAllByAtividadeIdInAndDataBetween(ids, inicio, fim).stream()
                .collect(
                        Collectors.groupingBy(
                                Conclusao::getAtividadeId,
                                Collectors.mapping(Conclusao::getData, Collectors.toSet())));
    }

    private static final class Acumulador {
        private int planejado;
        private int concluido;

        void somar(int planejadoDelta, int concluidoDelta) {
            planejado += planejadoDelta;
            concluido += concluidoDelta;
        }
    }
}
