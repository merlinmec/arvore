package dev.joaomanoel.arvore.util;

import dev.joaomanoel.arvore.dto.MetaDTO;
import dev.joaomanoel.arvore.dto.MetaDTO.SituacaoMeta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Regras puras da meta quantitativa de um objetivo, sem tocar banco. */
public final class MetaCalculo {

    /** Distância do ritmo ideal, em fração da meta, que ainda conta como "no ritmo". */
    static final BigDecimal TOLERANCIA = new BigDecimal("0.05");

    private static final BigDecimal SETE = BigDecimal.valueOf(7);

    private MetaCalculo() {}

    /**
     * @param inicio dia em que o objetivo foi criado: o ritmo ideal é uma reta de zero nesse dia até
     *     a meta no prazo (os dois dias inclusive)
     */
    public static MetaDTO calcular(
            BigDecimal meta,
            String unidade,
            BigDecimal acumulado,
            LocalDate inicio,
            LocalDate prazo,
            LocalDate hoje) {
        BigDecimal restante = meta.subtract(acumulado).max(BigDecimal.ZERO);
        int percentual =
                acumulado
                        .multiply(BigDecimal.valueOf(100))
                        .divide(meta, 0, RoundingMode.DOWN)
                        .min(BigDecimal.valueOf(100))
                        .intValue();

        if (prazo == null) {
            SituacaoMeta situacao =
                    restante.signum() == 0 ? SituacaoMeta.CONCLUIDA : SituacaoMeta.SEM_PRAZO;
            return new MetaDTO(meta, unidade, acumulado, percentual, restante, null, null, situacao);
        }

        LocalDate comeco = inicio.isAfter(prazo) ? prazo : inicio;
        long diasTotais = ChronoUnit.DAYS.between(comeco, prazo) + 1;
        long diasDecorridos = Math.max(0, Math.min(ChronoUnit.DAYS.between(comeco, hoje) + 1, diasTotais));
        BigDecimal esperado =
                meta.multiply(BigDecimal.valueOf(diasDecorridos))
                        .divide(BigDecimal.valueOf(diasTotais), 2, RoundingMode.HALF_UP);

        long diasRestantes = ChronoUnit.DAYS.between(hoje, prazo) + 1;
        BigDecimal ritmoSemanal = null;
        if (restante.signum() > 0 && diasRestantes > 0) {
            // Menos de uma semana até o prazo: o que falta cabe nessa semana.
            BigDecimal semanas =
                    BigDecimal.valueOf(diasRestantes).divide(SETE, 4, RoundingMode.HALF_UP).max(BigDecimal.ONE);
            ritmoSemanal = restante.divide(semanas, 1, RoundingMode.UP);
        }

        return new MetaDTO(
                meta,
                unidade,
                acumulado,
                percentual,
                restante,
                esperado,
                ritmoSemanal,
                situacao(meta, acumulado, restante, esperado, diasRestantes));
    }

    private static SituacaoMeta situacao(
            BigDecimal meta,
            BigDecimal acumulado,
            BigDecimal restante,
            BigDecimal esperado,
            long diasRestantes) {
        if (restante.signum() == 0) {
            return SituacaoMeta.CONCLUIDA;
        }
        if (diasRestantes <= 0) {
            return SituacaoMeta.VENCIDA;
        }
        BigDecimal folga = meta.multiply(TOLERANCIA);
        BigDecimal diferenca = acumulado.subtract(esperado);
        if (diferenca.compareTo(folga) > 0) {
            return SituacaoMeta.ADIANTADA;
        }
        if (diferenca.compareTo(folga.negate()) < 0) {
            return SituacaoMeta.ATRASADA;
        }
        return SituacaoMeta.NO_RITMO;
    }
}
