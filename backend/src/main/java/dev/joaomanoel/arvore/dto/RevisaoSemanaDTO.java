package dev.joaomanoel.arvore.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Retrato de uma semana (segunda a domingo) pra revisão. A contagem vai até {@code apuradoAte}: na
 * semana em curso, os dias que ainda não chegaram ficam de fora. {@code nota} é null enquanto a
 * semana não foi revisada.
 */
public record RevisaoSemanaDTO(
        LocalDate semana,
        LocalDate fim,
        LocalDate apuradoAte,
        int planejado,
        int concluido,
        Integer taxa,
        Integer taxaSemanaAnterior,
        List<DiaConsistenciaDTO> dias,
        List<AtividadeRevisaoDTO> atividades,
        String nota) {}
