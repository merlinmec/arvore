package dev.joaomanoel.arvore.util;

import dev.joaomanoel.arvore.dto.ProgressoDTO;

/**
 * Lógica pura de cálculo de progresso, sem tocar banco — testável isoladamente
 * (mesmo princípio do calcularProgressoArea no projeto mec-evolucao).
 */
public final class ProgressoCalculo {

    private ProgressoCalculo() {}

    public static ProgressoDTO calcular(int planejado, int concluido) {
        if (planejado == 0) {
            return new ProgressoDTO(0, concluido, null);
        }
        int percentual = (int) Math.round((concluido * 100.0) / planejado);
        return new ProgressoDTO(planejado, concluido, Math.min(percentual, 100));
    }
}
