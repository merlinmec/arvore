package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.AtividadeRevisaoDTO;
import dev.joaomanoel.arvore.dto.DiaConsistenciaDTO;
import dev.joaomanoel.arvore.dto.RevisaoDTO;
import dev.joaomanoel.arvore.dto.RevisaoSemanaDTO;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.Conclusao;
import dev.joaomanoel.arvore.entity.RevisaoSemanal;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.repository.RevisaoSemanalRepository;
import dev.joaomanoel.arvore.util.Consistencia;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revisão semanal: o retrato da semana (o que foi cumprido, o que escapou) e a nota do usuário. O
 * retrato é sempre recalculado pelas mesmas regras de {@link Consistencia}; só a nota é guardada.
 */
@Service
public class RevisaoService {

    private final CatalogoService catalogoService;
    private final ConclusaoRepository conclusaoRepository;
    private final RevisaoSemanalRepository revisaoRepository;

    public RevisaoService(
            CatalogoService catalogoService,
            ConclusaoRepository conclusaoRepository,
            RevisaoSemanalRepository revisaoRepository) {
        this.catalogoService = catalogoService;
        this.conclusaoRepository = conclusaoRepository;
        this.revisaoRepository = revisaoRepository;
    }

    /** A semana (segunda a domingo) que contém {@code data}, contada até hoje. */
    public RevisaoSemanaDTO semana(LocalDate data) {
        LocalDate hoje = Datas.hoje();
        LocalDate segunda = semanaJaComecada(data);
        LocalDate domingo = segunda.plusDays(6);
        LocalDate apuradoAte = domingo.isAfter(hoje) ? hoje : domingo;
        LocalDate segundaAnterior = segunda.minusWeeks(1);

        Catalogo catalogo = catalogoService.carregar();
        List<Atividade> atividades = catalogo.atividadesEmCurso(null);
        Map<UUID, Set<LocalDate>> concluidas = concluidas(atividades, segundaAnterior, apuradoAte);

        int[][] porDia = new int[7][2];
        int planejado = 0;
        int concluido = 0;
        int planejadoAnterior = 0;
        int concluidoAnterior = 0;
        List<AtividadeRevisaoDTO> porAtividade = new ArrayList<>();

        for (Atividade atividade : atividades) {
            Rotina rotina = catalogo.rotina(atividade.getId());
            Set<LocalDate> feitas = concluidas.getOrDefault(atividade.getId(), Set.of());

            int planejadoAtividade = 0;
            int concluidoAtividade = 0;
            List<LocalDate> perdidos = new ArrayList<>();
            for (LocalDate dia = segunda; !dia.isAfter(apuradoAte); dia = dia.plusDays(1)) {
                Consistencia.Dia resultado = Consistencia.dia(rotina, feitas, dia);
                int[] doDia = porDia[indice(segunda, dia)];
                doDia[0] += resultado.planejado();
                doDia[1] += resultado.concluido();
                planejadoAtividade += resultado.planejado();
                concluidoAtividade += resultado.concluido();
                // hoje ainda pode ser feito: só escapou o que ficou pra trás
                if (!resultado.cumprido() && dia.isBefore(hoje)) {
                    perdidos.add(dia);
                }
            }
            for (LocalDate dia = segundaAnterior; dia.isBefore(segunda); dia = dia.plusDays(1)) {
                Consistencia.Dia resultado = Consistencia.dia(rotina, feitas, dia);
                planejadoAnterior += resultado.planejado();
                concluidoAnterior += resultado.concluido();
            }
            planejado += planejadoAtividade;
            concluido += concluidoAtividade;

            Area area = catalogo.areaDe(atividade);
            porAtividade.add(
                    new AtividadeRevisaoDTO(
                            ObjetivoService.paraDTO(atividade, rotina),
                            catalogo.objetivoDe(atividade).getDescricao(),
                            area.getId(),
                            area.getNome(),
                            area.getCorTema(),
                            planejadoAtividade,
                            concluidoAtividade,
                            perdidos));
        }

        List<DiaConsistenciaDTO> dias = new ArrayList<>();
        for (LocalDate dia = segunda; !dia.isAfter(apuradoAte); dia = dia.plusDays(1)) {
            int[] doDia = porDia[indice(segunda, dia)];
            dias.add(new DiaConsistenciaDTO(dia, doDia[0], doDia[1]));
        }
        porAtividade.sort(
                Comparator.comparing(AtividadeRevisaoDTO::areaNome, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(revisao -> revisao.atividade().nome(), String.CASE_INSENSITIVE_ORDER));

        return new RevisaoSemanaDTO(
                segunda,
                domingo,
                apuradoAte,
                planejado,
                concluido,
                Consistencia.taxa(planejado, concluido),
                Consistencia.taxa(planejadoAnterior, concluidoAnterior),
                dias,
                porAtividade,
                revisaoRepository.findBySemana(segunda).map(RevisaoSemanal::getNota).orElse(null));
    }

    /** Grava (ou reescreve) a nota da semana que contém {@code data}. */
    @Transactional
    public RevisaoDTO salvar(LocalDate data, String nota) {
        LocalDate segunda = semanaJaComecada(data);
        RevisaoSemanal revisao =
                revisaoRepository
                        .findBySemana(segunda)
                        .orElseGet(
                                () -> {
                                    RevisaoSemanal nova = new RevisaoSemanal();
                                    nova.setSemana(segunda);
                                    return nova;
                                });
        revisao.setNota(nota.trim());
        return paraDTO(revisaoRepository.saveAndFlush(revisao));
    }

    /** Revisões já feitas, da mais recente pra mais antiga. */
    public List<RevisaoDTO> listar() {
        return revisaoRepository.findAllByOrderBySemanaDesc().stream().map(RevisaoService::paraDTO).toList();
    }

    private static LocalDate semanaJaComecada(LocalDate data) {
        LocalDate segunda = Datas.inicioDaSemana(data);
        if (segunda.isAfter(Datas.hoje())) {
            throw new IllegalArgumentException("Essa semana ainda não começou");
        }
        return segunda;
    }

    private static int indice(LocalDate segunda, LocalDate dia) {
        return (int) (dia.toEpochDay() - segunda.toEpochDay());
    }

    private Map<UUID, Set<LocalDate>> concluidas(List<Atividade> atividades, LocalDate inicio, LocalDate fim) {
        List<UUID> ids = atividades.stream().map(Atividade::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return conclusaoRepository.findAllByAtividadeIdInAndDataBetween(ids, inicio, fim).stream()
                .collect(
                        Collectors.groupingBy(
                                Conclusao::getAtividadeId,
                                Collectors.mapping(Conclusao::getData, Collectors.toSet())));
    }

    private static RevisaoDTO paraDTO(RevisaoSemanal revisao) {
        return new RevisaoDTO(revisao.getSemana(), revisao.getNota(), revisao.getAtualizadoEm());
    }
}
