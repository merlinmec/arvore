package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.DiaHojeDTO;
import dev.joaomanoel.arvore.dto.ItemHojeDTO;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.Conclusao;
import dev.joaomanoel.arvore.entity.Objetivo;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.util.Consistencia;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HojeService {

    private static final int JANELA_STREAK_DIAS = 180;

    private final CatalogoService catalogoService;
    private final ConclusaoRepository conclusaoRepository;

    public HojeService(CatalogoService catalogoService, ConclusaoRepository conclusaoRepository) {
        this.catalogoService = catalogoService;
        this.conclusaoRepository = conclusaoRepository;
    }

    /**
     * Plano do dia: atividades de objetivos ativos agendadas pro dia da semana de {@code data}, mais
     * as de meta semanal que ainda não bateram a meta (ou que foram feitas nesse dia).
     */
    public DiaHojeDTO listar(LocalDate data) {
        LocalDate hoje = Datas.hoje();
        Catalogo catalogo = catalogoService.carregar();

        List<Atividade> doDia =
                catalogo.atividadesEmCurso(null).stream()
                        .filter(atividade -> podeEntrarNoDia(catalogo.rotina(atividade.getId()), data))
                        .toList();

        // Começa numa segunda: a meta semanal olha a semana inteira de cada dia.
        LocalDate inicioJanela = Datas.inicioDaSemana(data.minusDays(JANELA_STREAK_DIAS));
        List<UUID> ids = doDia.stream().map(Atividade::getId).toList();
        Map<UUID, Set<LocalDate>> concluidasPorAtividade =
                ids.isEmpty()
                        ? Map.of()
                        : conclusaoRepository.findAllByAtividadeIdInAndDataBetween(ids, inicioJanela, data).stream()
                                .collect(
                                        Collectors.groupingBy(
                                                Conclusao::getAtividadeId,
                                                Collectors.mapping(Conclusao::getData, Collectors.toSet())));

        List<ItemHojeDTO> itens =
                doDia.stream()
                        .filter(
                                atividade ->
                                        aindaFaltaNaSemana(
                                                catalogo.rotina(atividade.getId()),
                                                concluidasPorAtividade.getOrDefault(atividade.getId(), Set.of()),
                                                data))
                        .map(
                                atividade -> {
                                    Set<LocalDate> concluidas =
                                            concluidasPorAtividade.getOrDefault(atividade.getId(), Set.of());
                                    return montarItem(
                                            catalogo, atividade, concluidas, data, inicioJanela);
                                })
                        .sorted(
                                Comparator.comparing(ItemHojeDTO::areaNome, String.CASE_INSENSITIVE_ORDER)
                                        .thenComparing(ItemHojeDTO::atividadeNome, String.CASE_INSENSITIVE_ORDER))
                        .toList();

        return new DiaHojeDTO(data, hoje, !data.isAfter(hoje), itens);
    }

    private ItemHojeDTO montarItem(
            Catalogo catalogo,
            Atividade atividade,
            Set<LocalDate> concluidas,
            LocalDate data,
            LocalDate inicioJanela) {
        Objetivo objetivo = catalogo.objetivoDe(atividade);
        Area area = catalogo.areaDe(atividade);
        Rotina rotina = catalogo.rotina(atividade.getId());
        Consistencia.Semana semana = Consistencia.semana(rotina, concluidas, data);

        return new ItemHojeDTO(
                atividade.getId(),
                atividade.getNome(),
                objetivo.getId(),
                objetivo.getDescricao(),
                area.getId(),
                area.getNome(),
                area.getCorTema(),
                concluidas.contains(data),
                Consistencia.sequenciaAtual(rotina, concluidas, data, inicioJanela),
                semana == null ? null : semana.meta(),
                semana == null ? null : semana.feitas());
    }

    private static boolean podeEntrarNoDia(Rotina rotina, LocalDate data) {
        return rotina.planejadaEm(data) || rotina.cotaEm(data) != null;
    }

    /** Meta semanal já batida em outros dias some da lista; feita nesse dia continua, pra poder desmarcar. */
    private static boolean aindaFaltaNaSemana(Rotina rotina, Set<LocalDate> concluidas, LocalDate data) {
        Consistencia.Semana semana = Consistencia.semana(rotina, concluidas, data);
        return semana == null || concluidas.contains(data) || semana.feitas() < semana.meta();
    }
}
