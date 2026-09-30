package dev.joaomanoel.arvore.dto;

import java.math.BigDecimal;

/**
 * Situação de uma meta quantitativa. {@code esperadoHoje} e {@code ritmoSemanal} só existem com
 * prazo: o primeiro é onde o acumulado estaria num ritmo constante desde a criação do objetivo, e o
 * segundo é quanto falta por semana pra chegar lá no prazo.
 */
public record MetaDTO(
        BigDecimal valor,
        String unidade,
        BigDecimal acumulado,
        int percentual,
        BigDecimal restante,
        BigDecimal esperadoHoje,
        BigDecimal ritmoSemanal,
        SituacaoMeta situacao) {

    public enum SituacaoMeta {
        CONCLUIDA,
        ADIANTADA,
        NO_RITMO,
        ATRASADA,
        VENCIDA,
        SEM_PRAZO
    }
}
