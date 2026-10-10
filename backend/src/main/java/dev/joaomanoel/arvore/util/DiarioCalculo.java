package dev.joaomanoel.arvore.util;

import dev.joaomanoel.arvore.dto.DiaConsistenciaDTO;
import dev.joaomanoel.arvore.dto.DiarioConsistenciaDTO.NivelDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Cruza o nível registrado no diário (humor ou energia) com a consistência de cada dia. */
public final class DiarioCalculo {

    public static final int NIVEL_MINIMO = 1;
    public static final int NIVEL_MAXIMO = 5;

    private DiarioCalculo() {}

    /**
     * Uma linha por nível, de 1 a 5, sempre as cinco. Dia sem nível registrado fica de fora; dia com
     * nível mas sem nada planejado conta em {@code dias} e não mexe na taxa.
     */
    public static List<NivelDTO> porNivel(Map<LocalDate, Integer> nivelPorData, List<DiaConsistenciaDTO> dias) {
        int tamanho = NIVEL_MAXIMO - NIVEL_MINIMO + 1;
        int[] contagem = new int[tamanho];
        int[] planejado = new int[tamanho];
        int[] concluido = new int[tamanho];
        for (DiaConsistenciaDTO dia : dias) {
            Integer nivel = nivelPorData.get(dia.data());
            if (nivel == null || nivel < NIVEL_MINIMO || nivel > NIVEL_MAXIMO) {
                continue;
            }
            int indice = nivel - NIVEL_MINIMO;
            contagem[indice]++;
            planejado[indice] += dia.planejado();
            concluido[indice] += dia.concluido();
        }
        List<NivelDTO> niveis = new ArrayList<>(tamanho);
        for (int indice = 0; indice < tamanho; indice++) {
            niveis.add(
                    new NivelDTO(
                            indice + NIVEL_MINIMO,
                            contagem[indice],
                            planejado[indice],
                            concluido[indice],
                            Consistencia.taxa(planejado[indice], concluido[indice])));
        }
        return niveis;
    }
}
