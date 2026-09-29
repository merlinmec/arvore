package dev.joaomanoel.arvore.util;

import dev.joaomanoel.arvore.dto.ProgressoDTO;
import dev.joaomanoel.arvore.entity.DiaSemana;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Regras puras de consistência (planejado x concluído, sequências), sem tocar banco.
 *
 * <p>"Planejado" é reconstruído a partir da grade semanal atual: a grade não é versionada, então
 * mudar os dias de uma atividade muda também o planejado dos dias passados. Dias anteriores à
 * criação da atividade nunca contam como planejados.
 */
public final class Consistencia {

    private Consistencia() {}

    /** Uma atividade vista como rotina: desde quando existe e em quais dias da semana ela cai. */
    public record Rotina(UUID atividadeId, LocalDate inicio, Set<DiaSemana> dias) {

        public boolean planejadaEm(LocalDate data) {
            return !data.isBefore(inicio) && dias.contains(Datas.diaSemanaDe(data));
        }
    }

    /**
     * Sequência atual até {@code referencia}: dias agendados concluídos em seguida. Se a referência
     * ainda não foi concluída, a contagem começa no dia anterior (o dia ainda pode ser feito).
     * Dias não agendados são pulados sem quebrar a sequência.
     */
    public static int sequenciaAtual(
            Rotina rotina, Set<LocalDate> concluidas, LocalDate referencia, LocalDate limite) {
        LocalDate data = concluidas.contains(referencia) ? referencia : referencia.minusDays(1);
        LocalDate piso = limite.isAfter(rotina.inicio()) ? limite : rotina.inicio();

        int sequencia = 0;
        while (!data.isBefore(piso)) {
            if (rotina.planejadaEm(data)) {
                if (!concluidas.contains(data)) {
                    break;
                }
                sequencia++;
            }
            data = data.minusDays(1);
        }
        return sequencia;
    }

    /** Maior sequência de dias agendados concluídos em seguida dentro de [inicio, fim]. */
    public static int maiorSequencia(
            Rotina rotina, Set<LocalDate> concluidas, LocalDate inicio, LocalDate fim) {
        int maior = 0;
        int atual = 0;
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            if (!rotina.planejadaEm(data)) {
                continue;
            }
            if (concluidas.contains(data)) {
                atual++;
                maior = Math.max(maior, atual);
            } else {
                atual = 0;
            }
        }
        return maior;
    }

    /**
     * Planejado x concluído das rotinas dentro de [inicio, fim]. Só conta como concluído o que caiu
     * num dia planejado, então o percentual nunca passa de 100%.
     */
    public static ProgressoDTO progresso(
            Collection<Rotina> rotinas,
            Map<UUID, Set<LocalDate>> concluidas,
            LocalDate inicio,
            LocalDate fim) {
        int planejado = 0;
        int concluido = 0;
        for (Rotina rotina : rotinas) {
            Set<LocalDate> feitas = concluidas.getOrDefault(rotina.atividadeId(), Set.of());
            for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
                if (rotina.planejadaEm(data)) {
                    planejado++;
                    if (feitas.contains(data)) {
                        concluido++;
                    }
                }
            }
        }
        return ProgressoCalculo.calcular(planejado, concluido);
    }

    /** Taxa inteira 0–100, ou null quando nada foi planejado. */
    public static Integer taxa(int planejado, int concluido) {
        return ProgressoCalculo.calcular(planejado, concluido).percentual();
    }
}
