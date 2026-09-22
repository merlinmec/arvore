package dev.joaomanoel.arvore.util;

import dev.joaomanoel.arvore.entity.DiaSemana;
import java.time.LocalDate;
import java.time.ZoneId;

public final class Datas {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Datas() {}

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    public static LocalDate inicioDaSemana() {
        LocalDate hoje = hoje();
        return hoje.minusDays(hoje.getDayOfWeek().getValue() - 1);
    }

    public static LocalDate fimDaSemana() {
        return inicioDaSemana().plusDays(6);
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
