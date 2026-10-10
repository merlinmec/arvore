package dev.joaomanoel.arvore.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void campoSimplesFicaComoEsta() {
        assertThat(Csv.campo("Correr 5 km")).isEqualTo("Correr 5 km");
        assertThat(Csv.campo(null)).isEmpty();
    }

    @Test
    void separadorAspasEQuebraDeLinhaPedemAspas() {
        assertThat(Csv.campo("a;b")).isEqualTo("\"a;b\"");
        assertThat(Csv.campo("disse \"oi\"")).isEqualTo("\"disse \"\"oi\"\"\"");
        assertThat(Csv.campo("linha 1\nlinha 2")).isEqualTo("\"linha 1\nlinha 2\"");
    }

    @Test
    void linhaJuntaComPontoEVirgulaETerminaEmCrlf() {
        assertThat(Csv.linha(Arrays.asList("2026-10-10", "Saúde / Corrida", null, "5,5")))
                .isEqualTo("2026-10-10;Saúde / Corrida;;5,5\r\n");
        assertThat(Csv.linha(List.of("só"))).isEqualTo("só\r\n");
    }

    @Test
    void numeroUsaVirgulaESemZerosSobrando() {
        assertThat(Csv.numero(new BigDecimal("5.50"))).isEqualTo("5,5");
        assertThat(Csv.numero(new BigDecimal("100.00"))).isEqualTo("100");
        assertThat(Csv.numero(new BigDecimal("0.00"))).isEqualTo("0");
        assertThat(Csv.numero(null)).isNull();
    }
}
