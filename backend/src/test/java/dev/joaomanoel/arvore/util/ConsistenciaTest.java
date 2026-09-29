package dev.joaomanoel.arvore.util;

import static org.assertj.core.api.Assertions.assertThat;

import dev.joaomanoel.arvore.dto.ProgressoDTO;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import dev.joaomanoel.arvore.util.Consistencia.Vigencia;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        return new Rotina(
                UUID.randomUUID(),
                inicio,
                Arrays.stream(dias).map(dia -> new Vigencia(dia, inicio, null)).toList());
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
    void mudarAGradeNaoReescreveOPassado() {
        // Treinava segunda até esta semana; a partir desta quarta passou a treinar quarta.
        Rotina trocouDeDia =
                new Rotina(
                        UUID.randomUUID(),
                        SEGUNDA.minusWeeks(4),
                        List.of(
                                new Vigencia(DiaSemana.SEGUNDA, SEGUNDA.minusWeeks(4), QUARTA),
                                new Vigencia(DiaSemana.QUARTA, QUARTA, null)));

        assertThat(trocouDeDia.planejadaEm(SEGUNDA.minusWeeks(1))).isTrue();
        assertThat(trocouDeDia.planejadaEm(QUARTA.minusWeeks(1))).isFalse();
        assertThat(trocouDeDia.planejadaEm(SEGUNDA)).isTrue();
        assertThat(trocouDeDia.planejadaEm(QUARTA)).isTrue();
        assertThat(trocouDeDia.planejadaEm(SEGUNDA.plusWeeks(1))).isFalse();
        assertThat(trocouDeDia.diasAtuais()).containsExactly(DiaSemana.QUARTA);
    }

    @Test
    void sequenciaAtravessaATrocaDeGrade() {
        Rotina trocouDeDia =
                new Rotina(
                        UUID.randomUUID(),
                        SEGUNDA.minusWeeks(4),
                        List.of(
                                new Vigencia(DiaSemana.SEGUNDA, SEGUNDA.minusWeeks(4), QUARTA),
                                new Vigencia(DiaSemana.QUARTA, QUARTA, null)));
        Set<LocalDate> feitas = Set.of(SEGUNDA.minusWeeks(1), SEGUNDA, QUARTA);

        assertThat(Consistencia.sequenciaAtual(trocouDeDia, feitas, QUARTA, LIMITE)).isEqualTo(3);
    }

    @Test
    void diasAtuaisSaemDeSegundaADomingo() {
        Rotina rotina = rotina(SEGUNDA, DiaSemana.DOMINGO, DiaSemana.SEGUNDA, DiaSemana.QUARTA);

        assertThat(rotina.diasAtuais())
                .containsExactly(DiaSemana.SEGUNDA, DiaSemana.QUARTA, DiaSemana.DOMINGO);
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
    void progressoSomaAsRotinasEIgnoraConclusaoForaDoPlano() {
        Rotina diaria = rotina(SEGUNDA.minusWeeks(1), DiaSemana.values());
        Map<UUID, Set<LocalDate>> concluidas =
                Map.of(
                        segQuaSex.atividadeId(), Set.of(SEGUNDA, SEGUNDA.plusDays(1)), // terça não conta
                        diaria.atividadeId(), Set.of(SEGUNDA, QUARTA, SEXTA));

        ProgressoDTO progresso =
                Consistencia.progresso(List.of(segQuaSex, diaria), concluidas, SEGUNDA, SEGUNDA.plusDays(6));

        assertThat(progresso.planejado()).isEqualTo(3 + 7);
        assertThat(progresso.concluido()).isEqualTo(1 + 3);
        assertThat(progresso.percentual()).isEqualTo(40);
    }

    @Test
    void progressoNaoPlanejaDiasAntesDaCriacao() {
        Rotina nova = rotina(QUARTA, DiaSemana.SEGUNDA, DiaSemana.QUARTA, DiaSemana.SEXTA);

        ProgressoDTO progresso =
                Consistencia.progresso(List.of(nova), Map.of(), SEGUNDA, SEGUNDA.plusDays(6));

        assertThat(progresso.planejado()).isEqualTo(2);
        assertThat(progresso.percentual()).isZero();
    }

    @Test
    void progressoSemRotinasNaoTemPercentual() {
        assertThat(Consistencia.progresso(List.of(), Map.of(), SEGUNDA, SEXTA).percentual()).isNull();
    }

    @Test
    void taxaNulaQuandoNadaFoiPlanejado() {
        assertThat(Consistencia.taxa(0, 0)).isNull();
        assertThat(Consistencia.taxa(4, 3)).isEqualTo(75);
    }
}
