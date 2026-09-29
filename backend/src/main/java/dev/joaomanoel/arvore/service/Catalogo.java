package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.Objetivo;
import dev.joaomanoel.arvore.entity.StatusObjetivo;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import dev.joaomanoel.arvore.util.Consistencia.Vigencia;
import dev.joaomanoel.arvore.util.Datas;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Retrato em memória da árvore inteira (áreas, objetivos, atividades e grade semanal), carregado
 * em quatro queries por {@link CatalogoService}. Existe pra nenhuma tela fazer uma query por
 * atividade: o app é single-user, então o volume cabe folgado em memória. Listas preservam a ordem
 * de carga (áreas por {@code ordem}).
 */
public class Catalogo {

    private final Map<UUID, Area> areas;
    private final Map<UUID, Objetivo> objetivos;
    private final Map<UUID, Atividade> atividades;
    private final Map<UUID, Rotina> rotinas;
    private final Map<UUID, List<Area>> filhosPorPai;
    private final Map<UUID, List<Objetivo>> objetivosPorArea;
    private final Map<UUID, List<Atividade>> atividadesPorObjetivo;

    Catalogo(
            Collection<Area> areas,
            Collection<Objetivo> objetivos,
            Collection<Atividade> atividades,
            Collection<AgendaSemanal> agendas) {
        this.areas = indexar(areas, Area::getId);
        this.objetivos = indexar(objetivos, Objetivo::getId);
        this.atividades = indexar(atividades, Atividade::getId);
        this.filhosPorPai =
                areas.stream()
                        .filter(area -> area.getAreaPaiId() != null)
                        .collect(Collectors.groupingBy(Area::getAreaPaiId));
        this.objetivosPorArea = objetivos.stream().collect(Collectors.groupingBy(Objetivo::getAreaId));
        this.atividadesPorObjetivo =
                atividades.stream().collect(Collectors.groupingBy(Atividade::getObjetivoId));

        Map<UUID, List<Vigencia>> gradePorAtividade =
                agendas.stream()
                        .collect(
                                Collectors.groupingBy(
                                        AgendaSemanal::getAtividadeId,
                                        Collectors.mapping(
                                                agenda ->
                                                        new Vigencia(
                                                                agenda.getDiaSemana(),
                                                                agenda.getVigenteDe(),
                                                                agenda.getVigenteAte()),
                                                Collectors.toList())));
        this.rotinas =
                atividades.stream()
                        .collect(
                                Collectors.toMap(
                                        Atividade::getId,
                                        atividade ->
                                                new Rotina(
                                                        atividade.getId(),
                                                        Datas.dataDe(atividade.getCriadoEm()),
                                                        gradePorAtividade.getOrDefault(
                                                                atividade.getId(), List.of()))));
    }

    private static <T> Map<UUID, T> indexar(Collection<T> itens, Function<T, UUID> id) {
        return itens.stream()
                .collect(
                        Collectors.toMap(
                                id, Function.identity(), (primeiro, segundo) -> primeiro, LinkedHashMap::new));
    }

    /** Áreas sem pai, na ordem em que foram carregadas. */
    public List<Area> raizes() {
        return areas.values().stream().filter(area -> area.getAreaPaiId() == null).toList();
    }

    public List<Area> filhosDe(UUID areaId) {
        return filhosPorPai.getOrDefault(areaId, List.of());
    }

    /** Objetivos diretos da área, sem os de subáreas. */
    public List<Objetivo> objetivosDe(UUID areaId) {
        return objetivosPorArea.getOrDefault(areaId, List.of());
    }

    public List<Atividade> atividadesDe(UUID objetivoId) {
        return atividadesPorObjetivo.getOrDefault(objetivoId, List.of());
    }

    public List<Rotina> rotinasDe(Objetivo objetivo) {
        return atividadesDe(objetivo.getId()).stream().map(atividade -> rotina(atividade.getId())).toList();
    }

    public Area area(UUID id) {
        return areas.get(id);
    }

    public boolean existeArea(UUID id) {
        return areas.containsKey(id);
    }

    public Objetivo objetivoDe(Atividade atividade) {
        return objetivos.get(atividade.getObjetivoId());
    }

    public Area areaDe(Atividade atividade) {
        return areas.get(objetivoDe(atividade).getAreaId());
    }

    public Rotina rotina(UUID atividadeId) {
        return rotinas.get(atividadeId);
    }

    /**
     * Atividades que contam como plano: objetivo ATIVO e, havendo filtro, dentro da área filtrada ou
     * de qualquer descendente dela.
     */
    public List<Atividade> atividadesEmCurso(UUID areaFiltro) {
        Set<UUID> areasPermitidas = areaFiltro == null ? null : comDescendentes(areaFiltro);
        return atividades.values().stream()
                .filter(atividade -> objetivoDe(atividade).getStatus() == StatusObjetivo.ATIVO)
                .filter(
                        atividade ->
                                areasPermitidas == null
                                        || areasPermitidas.contains(areaDe(atividade).getId()))
                .toList();
    }

    public Set<UUID> comDescendentes(UUID areaId) {
        Set<UUID> resultado = new HashSet<>();
        Deque<UUID> pendentes = new ArrayDeque<>(List.of(areaId));
        while (!pendentes.isEmpty()) {
            UUID atual = pendentes.pop();
            if (resultado.add(atual)) {
                filhosPorPai.getOrDefault(atual, List.of()).forEach(filho -> pendentes.push(filho.getId()));
            }
        }
        return resultado;
    }

    /**
     * O galho em que uma área se agrupa num gráfico. Sem filtro, é a raiz da árvore; com filtro, é o
     * filho direto do filtro no caminho até ela (ou o próprio filtro, quando a área é ele).
     */
    public Area ramoDe(UUID areaId, UUID areaFiltro) {
        Area atual = areas.get(areaId);
        while (!atual.getId().equals(areaFiltro)
                && atual.getAreaPaiId() != null
                && !atual.getAreaPaiId().equals(areaFiltro)) {
            atual = areas.get(atual.getAreaPaiId());
        }
        return atual;
    }
}
