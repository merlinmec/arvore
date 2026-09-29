package dev.joaomanoel.arvore.util;

import static org.assertj.core.api.Assertions.assertThat;

import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsistenciaTest {

    // 2026-09-21 é uma segunda-feira
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 9, 21);
    private static final LocalDate QUARTA = SEGUNDA.plusDays(2);
    private static final LocalDate SEXTA = SEGUNDA.plusDays(4);
    private static final LocalDate LIMITE = SEGUNDA.minusDays(180);

    private static Rotina rotina(LocalDate inicio, DiaSemana... dias) {
        return new Rotina(UUID.randomUUID(), inicio, EnumSet.copyOf(Set.of(dias)));
    }

    private final Rotina segQuaSex =
            rotina(SEGUNDA.minusWeeks(4), DiaSemana.SEGUNDA, DiaSemana.QUARTA, DiaSemana.SEXTA);

    @Test
    void naoContaComoPlanejadoAntesDaCriacaoDaAtividade() {
        Rotina nova = rotina(QUARTA, DiaSemana.SEGUNDA, DiaSemana.QUARTA);

        assertThat(nova.planejadaEm(SEGUNDA)).isFalse();
        assertThat(nova.planejadaEm(QUARTA)).isTrue();
    }

    @Test
    void diasNaoAgendadosNaoQuebramASequencia() {
        Set<LocalDate> feitas = Set.of(SEGUNDA, QUARTA, SEXTA);

        assertThat(Consistencia.sequenciaAtual(segQuaSex, feitas, SEXTA, LIMITE)).isEqualTo(3);
    }

    @Test
    void referenciaAindaNaoFeitaNaoZeraASequencia() {
        Set<LocalDate> feitas = Set.of(SEGUNDA, QUARTA);

        assertThat(Consistencia.sequenciaAtual(segQuaSex, feitas, SEXTA, LIMITE)).isEqualTo(2);
    }

    @Test
    void diaAgendadoPerdidoQuebraASequencia() {
        Set<LocalDate> feitas = Set.of(SEGUNDA.minusWeeks(1), SEXTA);

        assertThat(Consistencia.sequenciaAtual(segQuaSex, feitas, SEXTA, LIMITE)).isEqualTo(1);
    }

    @Test
    void sequenciaParaNaCriacaoDaAtividade() {
        Rotina nova = rotina(SEGUNDA, DiaSemana.SEGUNDA, DiaSemana.QUARTA);

        assertThat(Consistencia.sequenciaAtual(nova, Set.of(SEGUNDA, QUARTA), QUARTA, LIMITE)).isEqualTo(2);
    }

    @Test
    void maiorSequenciaEncontraOTrechoMaisLongo() {
        LocalDate semanaPassada = SEGUNDA.minusWeeks(1);
        Set<LocalDate> feitas =
                Set.of(
                        semanaPassada,
                        semanaPassada.plusDays(2),
                        semanaPassada.plusDays(4),
                        SEGUNDA, // perdeu a quarta dessa semana
                        SEXTA);

        assertThat(Consistencia.maiorSequencia(segQuaSex, feitas, semanaPassada, SEXTA)).isEqualTo(4);
    }

    @Test
    void taxaNulaQuandoNadaFoiPlanejado() {
        assertThat(Consistencia.taxa(0, 0)).isNull();
        assertThat(Consistencia.taxa(4, 3)).isEqualTo(75);
    }
}
