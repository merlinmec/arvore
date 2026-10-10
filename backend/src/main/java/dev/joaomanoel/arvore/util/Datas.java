package dev.joaomanoel.arvore.util;

import dev.joaomanoel.arvore.entity.DiaSemana;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class Datas {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Datas() {}

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    public static LocalDate inicioDaSemana() {
        return inicioDaSemana(hoje());
    }

    /** Segunda-feira da semana que contém {@code data}. */
    public static LocalDate inicioDaSemana(LocalDate data) {
        return data.minusDays(data.getDayOfWeek().getValue() - 1);
    }

    public static LocalDate fimDaSemana() {
        return inicioDaSemana().plusDays(6);
    }

    public static ZoneId fuso() {
        return FUSO;
    }

    /** Meia-noite do dia, no fuso do app. */
    public static Instant inicioDoDia(LocalDate data) {
        return data.atStartOfDay(FUSO).toInstant();
    }

    /** Dia civil, no fuso do app, em que o instante aconteceu. */
    public static LocalDate dataDe(Instant instante) {
        return instante.atZone(FUSO).toLocalDate();
    }

    public static DiaSemana diaSemanaDe(LocalDate data) {
        return switch (data.getDayOfWeek()) {
            case MONDAY -> DiaSemana.SEGUNDA;
            case TUESDAY -> DiaSemana.TERCA;
            case WEDNESDAY -> DiaSemana.QUARTA;
            case THURSDAY -> DiaSemana.QUINTA;
            case FRIDAY -> DiaSemana.SEXTA;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }
}
