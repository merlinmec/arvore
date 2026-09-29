package dev.joaomanoel.arvore.util;

import dev.joaomanoel.arvore.dto.ProgressoDTO;
import dev.joaomanoel.arvore.entity.DiaSemana;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Regras puras de consistência (planejado x concluído, sequências), sem tocar banco.
 *
 * <p>"Planejado" é reconstruído a partir do histórico da grade: cada regra vale dentro da sua
 * vigência, então mudar a grade hoje não altera o planejado dos dias passados. Dias anteriores à
 * criação da atividade nunca contam como planejados.
 *
 * <p>Uma atividade segue dias fixos ({@link Vigencia}) ou uma meta de N vezes por semana ({@link
 * Cota}). Tudo é contado dia a dia por {@link #dia}, pra heatmap, taxa, streak e progresso somarem
 * do mesmo jeito nos dois casos.
 */
public final class Consistencia {

    private Consistencia() {}

    /** Um dia da semana na grade, valendo de {@code de} até o dia anterior a {@code ate} (null = em vigor). */
    public record Vigencia(DiaSemana dia, LocalDate de, LocalDate ate) {

        public boolean cobre(LocalDate data) {
            return dia == Datas.diaSemanaDe(data) && dentro(data, de, ate);
        }
    }

    /** Meta de {@code vezes} por semana, em qualquer dia, valendo de {@code de} até o dia anterior a {@code ate}. */
    public record Cota(int vezes, LocalDate de, LocalDate ate) {

        public boolean cobre(LocalDate data) {
            return dentro(data, de, ate);
        }
    }

    private static boolean dentro(LocalDate data, LocalDate de, LocalDate ate) {
        return !data.isBefore(de) && (ate == null || data.isBefore(ate));
    }

    /** Uma atividade vista como rotina: desde quando existe e o histórico da sua grade. */
    public record Rotina(UUID atividadeId, LocalDate inicio, List<Vigencia> grade, List<Cota> cotas) {

        public Rotina(UUID atividadeId, LocalDate inicio, List<Vigencia> grade) {
            this(atividadeId, inicio, grade, List.of());
        }

        /** Se {@code data} é um dos dias fixos da grade. */
        public boolean planejadaEm(LocalDate data) {
            return !data.isBefore(inicio) && grade.stream().anyMatch(vigencia -> vigencia.cobre(data));
        }

        /** A meta semanal que vale em {@code data}, se a atividade estava no modo "N vezes por semana". */
        public Cota cotaEm(LocalDate data) {
            if (data.isBefore(inicio)) {
                return null;
            }
            return cotas.stream().filter(cota -> cota.cobre(data)).findFirst().orElse(null);
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

        /** Meta semanal em vigor, ou null quando a atividade segue dias fixos. */
        public Integer vezesAtuais() {
            return cotas.stream()
                    .filter(cota -> cota.ate() == null)
                    .map(Cota::vezes)
                    .findFirst()
                    .orElse(null);
        }
    }

    /** Planejado x concluído de uma atividade num único dia. */
    public record Dia(int planejado, int concluido) {

        static final Dia LIVRE = new Dia(0, 0);

        public boolean cumprido() {
            return concluido >= planejado;
        }
    }

    /** Situação da meta semanal na semana de uma data. */
    public record Semana(int meta, int feitas) {}

    /**
     * Quanto a atividade tinha planejado e cumpriu em {@code data}.
     *
     * <p>Dia fixo: planejado 1, concluído se foi feito. Meta semanal: um dia feito conta 1 de 1 (até
     * a meta da semana), e cada vez que falta vira falha no dia em que deixou de dar tempo. Com 3x
     * por semana e nada feito, as falhas caem na sexta, no sábado e no domingo. Somando a semana
     * inteira, o planejado é a meta; e nenhum dia vira falha enquanto ainda dá pra cumprir.
     */
    public static Dia dia(Rotina rotina, Set<LocalDate> concluidas, LocalDate data) {
        if (rotina.planejadaEm(data)) {
            return new Dia(1, concluidas.contains(data) ? 1 : 0);
        }
        if (rotina.cotaEm(data) == null) {
            return Dia.LIVRE;
        }
        List<LocalDate> cobertos = diasComCota(rotina, data);
        int meta = metaDaSemana(rotina, cobertos);
        int indice = cobertos.indexOf(data);

        int feitasAntes = 0;
        for (int i = 0; i < indice; i++) {
            if (concluidas.contains(cobertos.get(i))) {
                feitasAntes++;
            }
        }
        boolean contaHoje = concluidas.contains(data) && feitasAntes < meta;
        int feitasAteHoje = Math.min(meta, feitasAntes + (concluidas.contains(data) ? 1 : 0));
        int restantesDepois = cobertos.size() - indice - 1;
        int restantesAntes = restantesDepois + 1;

        int perdidasAteOntem = Math.max(0, meta - Math.min(meta, feitasAntes) - restantesAntes);
        int perdidasAteHoje = Math.max(0, meta - feitasAteHoje - restantesDepois);
        int perdidasHoje = perdidasAteHoje - perdidasAteOntem;

        int concluido = contaHoje ? 1 : 0;
        return new Dia(concluido + perdidasHoje, concluido);
    }

    /**
     * Meta e vezes feitas na semana de {@code data}, contando só até {@code data}; null quando a
     * atividade não estava no modo "N vezes por semana" nesse dia.
     */
    public static Semana semana(Rotina rotina, Set<LocalDate> concluidas, LocalDate data) {
        if (rotina.cotaEm(data) == null) {
            return null;
        }
        List<LocalDate> cobertos = diasComCota(rotina, data);
        int feitas =
                (int) cobertos.stream().filter(dia -> !dia.isAfter(data)).filter(concluidas::contains).count();
        return new Semana(metaDaSemana(rotina, cobertos), feitas);
    }

    /** Dias da semana de {@code data} (segunda a domingo) em que alguma meta semanal valia. */
    private static List<LocalDate> diasComCota(Rotina rotina, LocalDate data) {
        LocalDate segunda = Datas.inicioDaSemana(data);
        List<LocalDate> cobertos = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate dia = segunda.plusDays(i);
            if (rotina.cotaEm(dia) != null) {
                cobertos.add(dia);
            }
        }
        return cobertos;
    }

    /**
     * A meta da semana é a da última regra que valeu nela (mudar de 3x pra 4x na quarta faz a semana
     * inteira valer 4x), limitada aos dias em que dava pra cumprir.
     */
    private static int metaDaSemana(Rotina rotina, List<LocalDate> cobertos) {
        int vezes = rotina.cotaEm(cobertos.get(cobertos.size() - 1)).vezes();
        return Math.min(vezes, cobertos.size());
    }

    /**
     * Sequência atual até {@code referencia}: vezes cumpridas em seguida. Se a referência ainda não
     * foi concluída, a contagem começa no dia anterior (o dia ainda pode ser feito). Dias sem plano
     * são pulados sem quebrar a sequência.
     */
    public static int sequenciaAtual(
            Rotina rotina, Set<LocalDate> concluidas, LocalDate referencia, LocalDate limite) {
        LocalDate data = concluidas.contains(referencia) ? referencia : referencia.minusDays(1);
        LocalDate piso = limite.isAfter(rotina.inicio()) ? limite : rotina.inicio();

        int sequencia = 0;
        while (!data.isBefore(piso)) {
            Dia dia = dia(rotina, concluidas, data);
            if (!dia.cumprido()) {
                break;
            }
            sequencia += dia.concluido();
            data = data.minusDays(1);
        }
        return sequencia;
    }

    /** Maior sequência de vezes cumpridas em seguida dentro de [inicio, fim]. */
    public static int maiorSequencia(
            Rotina rotina, Set<LocalDate> concluidas, LocalDate inicio, LocalDate fim) {
        int maior = 0;
        int atual = 0;
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            Dia dia = dia(rotina, concluidas, data);
            atual += dia.concluido();
            maior = Math.max(maior, atual);
            if (!dia.cumprido()) {
                atual = 0;
            }
        }
        return maior;
    }

    /**
     * Planejado x concluído das rotinas dentro de [inicio, fim]. Só conta como concluído o que
     * estava no plano, então o percentual nunca passa de 100%.
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
                Dia dia = dia(rotina, feitas, data);
                planejado += dia.planejado();
                concluido += dia.concluido();
            }
        }
        return ProgressoCalculo.calcular(planejado, concluido);
    }

    /** Taxa inteira 0–100, ou null quando nada foi planejado. */
    public static Integer taxa(int planejado, int concluido) {
        return ProgressoCalculo.calcular(planejado, concluido).percentual();
    }
}
