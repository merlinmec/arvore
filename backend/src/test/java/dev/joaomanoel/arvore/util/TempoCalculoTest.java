package dev.joaomanoel.arvore.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TempoCalculoTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DIA = LocalDate.of(2026, 10, 5);
    private static final Instant SEMPRE = Instant.EPOCH;
    private static final Instant NUNCA = Instant.parse("2100-01-01T00:00:00Z");

    private static Instant as(LocalDate dia, int hora, int minuto) {
        return LocalDateTime.of(dia, java.time.LocalTime.of(hora, minuto)).atZone(FUSO).toInstant();
    }

    @Test
    void sessaoNoMesmoDiaFicaInteiraNele() {
        Map<LocalDate, Long> porDia = TempoCalculo.segundosPorDia(as(DIA, 9, 0), as(DIA, 9, 45), SEMPRE, NUNCA, FUSO);

        assertThat(porDia).containsExactly(Map.entry(DIA, 45 * 60L));
    }

    @Test
    void sessaoQueVaraAMeiaNoiteSeDivideNoFusoDoApp() {
        Map<LocalDate, Long> porDia =
                TempoCalculo.segundosPorDia(as(DIA, 23, 0), as(DIA.plusDays(1), 1, 30), SEMPRE, NUNCA, FUSO);

        assertThat(porDia).containsEntry(DIA, 60 * 60L).containsEntry(DIA.plusDays(1), 90 * 60L).hasSize(2);
    }

    @Test
    void recortaAoPeriodoPedido() {
        Map<LocalDate, Long> porDia = TempoCalculo.segundosPorDia(
                as(DIA, 23, 0), as(DIA.plusDays(1), 1, 0), as(DIA.plusDays(1), 0, 0), NUNCA, FUSO);

        assertThat(porDia).containsExactly(Map.entry(DIA.plusDays(1), 60 * 60L));
        assertThat(TempoCalculo.segundosPorDia(as(DIA, 8, 0), as(DIA, 9, 0), as(DIA, 10, 0), NUNCA, FUSO))
                .isEmpty();
    }

    @Test
    void sessaoAbertaContaAteAgoraESessaoEsquecidaParaEm12Horas() {
        Instant inicio = as(DIA, 8, 0);

        assertThat(TempoCalculo.fimEfetivo(inicio, null, as(DIA, 8, 20))).isEqualTo(as(DIA, 8, 20));
        assertThat(TempoCalculo.fimEfetivo(inicio, as(DIA, 9, 0), as(DIA, 22, 0))).isEqualTo(as(DIA, 9, 0));
        assertThat(TempoCalculo.fimEfetivo(inicio, null, as(DIA.plusDays(2), 8, 0))).isEqualTo(as(DIA, 20, 0));
    }

    @Test
    void minutosArredondam() {
        assertThat(TempoCalculo.minutos(0)).isZero();
        assertThat(TempoCalculo.minutos(89)).isEqualTo(1);
        assertThat(TempoCalculo.minutos(90)).isEqualTo(2);
        assertThat(TempoCalculo.minutos(3600)).isEqualTo(60);
    }
}
