package dev.joaomanoel.arvore.util;

import dev.joaomanoel.arvore.dto.ProgressoDTO;
import dev.joaomanoel.arvore.entity.DiaSemana;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Regras puras de consistência (planejado x concluído, sequências), sem tocar banco.
 *
 * <p>"Planejado" é reconstruído a partir do histórico da grade semanal: cada dia da semana vale
 * dentro da sua vigência, então mudar a grade hoje não altera o planejado dos dias passados. Dias
 * anteriores à criação da atividade nunca contam como planejados.
 */
public final class Consistencia {

    private Consistencia() {}

    /** Um dia da semana na grade, valendo de {@code de} até o dia anterior a {@code ate} (null = em vigor). */
    public record Vigencia(DiaSemana dia, LocalDate de, LocalDate ate) {

        public boolean cobre(LocalDate data) {
            return dia == Datas.diaSemanaDe(data)
                    && !data.isBefore(de)
                    && (ate == null || data.isBefore(ate));
        }
    }

    /** Uma atividade vista como rotina: desde quando existe e o histórico da sua grade semanal. */
    public record Rotina(UUID atividadeId, LocalDate inicio, List<Vigencia> grade) {

        public boolean planejadaEm(LocalDate data) {
            return !data.isBefore(inicio) && grade.stream().anyMatch(vigencia -> vigencia.cobre(data));
        }

        /** Dias da grade em vigor, de segunda a domingo. */
        public List<DiaSemana> diasAtuais() {
            return grade.stream()
                    .filter(vigencia -> vigencia.ate() == null)
                    .map(Vigencia::dia)
                    .distinct()
                    .sorted()
                    .toList();
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
