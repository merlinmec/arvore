package dev.joaomanoel.arvore.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Consistência de um período. {@code taxaPeriodoAnterior} cobre a janela de mesmo tamanho logo
 * antes de {@code inicio}, pra comparação.
 */
public record ResumoInsightsDTO(
        LocalDate inicio,
        LocalDate fim,
        int planejado,
        int concluido,
        Integer taxa,
        Integer taxaPeriodoAnterior,
        int diasComPlano,
        int diasPerfeitos,
        List<SerieDiaDTO> serie,
        List<AreaConsistenciaDTO> porArea,
        List<DiaSemanaConsistenciaDTO> porDiaSemana,
        List<AtividadeConsistenciaDTO> atividades) {}
