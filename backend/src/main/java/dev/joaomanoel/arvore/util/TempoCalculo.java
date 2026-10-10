package dev.joaomanoel.arvore.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/** Regras do tempo de foco: duração de uma sessão e como ela se divide entre os dias. */
public final class TempoCalculo {

    /** Menos que isso ao parar é clique sem querer: a sessão é descartada. */
    public static final Duration SESSAO_MINIMA = Duration.ofMinutes(1);

    /** Sessão esquecida aberta não vira um dia inteiro de foco: passa disso, é cortada. */
    public static final Duration SESSAO_MAXIMA = Duration.ofHours(12);

    private TempoCalculo() {}

    /** Onde a sessão termina pra efeito de contagem: no fim, em "agora" se está aberta, e no máximo 12 h. */
    public static Instant fimEfetivo(Instant inicio, Instant fim, Instant agora) {
        Instant ate = fim != null ? fim : agora;
        Instant limite = inicio.plus(SESSAO_MAXIMA);
        return ate.isAfter(limite) ? limite : ate;
    }

    /**
     * Segundos da sessão em cada dia civil do fuso, recortados a [{@code de}, {@code ate}). Uma sessão
     * das 23h à 1h conta 1 h em cada dia.
     */
    public static Map<LocalDate, Long> segundosPorDia(
            Instant inicio, Instant fim, Instant de, Instant ate, ZoneId fuso) {
        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        Instant cursor = inicio.isBefore(de) ? de : inicio;
        Instant termino = fim.isAfter(ate) ? ate : fim;
        while (cursor.isBefore(termino)) {
            LocalDate dia = cursor.atZone(fuso).toLocalDate();
            Instant proximaMeiaNoite = dia.plusDays(1).atStartOfDay(fuso).toInstant();
            Instant fimDoTrecho = termino.isBefore(proximaMeiaNoite) ? termino : proximaMeiaNoite;
            porDia.merge(dia, Duration.between(cursor, fimDoTrecho).toSeconds(), Long::sum);
            cursor = fimDoTrecho;
        }
        return porDia;
    }

    /** Minutos inteiros, arredondados: 89 s contam 1 min, 90 s contam 2. */
    public static int minutos(long segundos) {
        return (int) Math.round(segundos / 60.0);
    }
}
