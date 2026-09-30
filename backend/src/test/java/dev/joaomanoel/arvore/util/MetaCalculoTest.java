package dev.joaomanoel.arvore.util;

import static org.assertj.core.api.Assertions.assertThat;

import dev.joaomanoel.arvore.dto.MetaDTO;
import dev.joaomanoel.arvore.dto.MetaDTO.SituacaoMeta;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MetaCalculoTest {

    // 100 km de 1º de outubro a 9 de janeiro: 101 dias.
    private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);
    private static final LocalDate PRAZO = LocalDate.of(2027, 1, 9);
    private static final BigDecimal CEM = new BigDecimal("100");

    private static MetaDTO calcular(String acumulado, LocalDate prazo, LocalDate hoje) {
        return MetaCalculo.calcular(CEM, "km", new BigDecimal(acumulado), INICIO, prazo, hoje);
    }

    @Test
    void naMetadeDoPrazoComMetadeFeitaEstaNoRitmo() {
        MetaDTO meta = calcular("50", PRAZO, INICIO.plusDays(50)); // 51 de 101 dias

        assertThat(meta.situacao()).isEqualTo(SituacaoMeta.NO_RITMO);
        assertThat(meta.percentual()).isEqualTo(50);
        assertThat(meta.restante()).isEqualByComparingTo("50");
        assertThat(meta.esperadoHoje()).isEqualByComparingTo("50.50");
    }

    @Test
    void foraDaToleranciaFicaAdiantadaOuAtrasada() {
        LocalDate metade = INICIO.plusDays(50);

        assertThat(calcular("60", PRAZO, metade).situacao()).isEqualTo(SituacaoMeta.ADIANTADA);
        assertThat(calcular("40", PRAZO, metade).situacao()).isEqualTo(SituacaoMeta.ATRASADA);
    }

    @Test
    void ritmoSemanalDivideORestantePelasSemanasQueFaltam() {
        // Faltam 14 dias (contando hoje) e 30 km: 15 km por semana.
        MetaDTO meta = calcular("70", PRAZO, PRAZO.minusDays(13));

        assertThat(meta.ritmoSemanal()).isEqualByComparingTo("15");
    }

    @Test
    void menosDeUmaSemanaPedeTudoQueFaltaNessaSemana() {
        MetaDTO meta = calcular("90", PRAZO, PRAZO.minusDays(2));

        assertThat(meta.ritmoSemanal()).isEqualByComparingTo("10");
    }

    @Test
    void metaBatidaEstaConcluidaMesmoPassandoDoValor() {
        MetaDTO meta = calcular("120", PRAZO, INICIO.plusDays(10));

        assertThat(meta.situacao()).isEqualTo(SituacaoMeta.CONCLUIDA);
        assertThat(meta.percentual()).isEqualTo(100);
        assertThat(meta.restante()).isEqualByComparingTo("0");
        assertThat(meta.ritmoSemanal()).isNull();
    }

    @Test
    void prazoPassadoSemBaterAMetaEstaVencida() {
        MetaDTO meta = calcular("80", PRAZO, PRAZO.plusDays(1));

        assertThat(meta.situacao()).isEqualTo(SituacaoMeta.VENCIDA);
        assertThat(meta.ritmoSemanal()).isNull();
    }

    @Test
    void semPrazoNaoTemRitmo() {
        MetaDTO meta = calcular("30", null, INICIO.plusDays(10));

        assertThat(meta.situacao()).isEqualTo(SituacaoMeta.SEM_PRAZO);
        assertThat(meta.esperadoHoje()).isNull();
        assertThat(meta.ritmoSemanal()).isNull();
    }
}
