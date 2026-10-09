package dev.joaomanoel.arvore.dto;

import java.util.UUID;

public record AtividadeConsistenciaDTO(
        UUID atividadeId,
        String nome,
        String objetivoDescricao,
        UUID areaId,
        String areaNome,
        String areaCorTema,
        int planejado,
        int concluido,
        Integer taxa,
        int streakAtual,
        int maiorSequencia,
        // força do hábito hoje (0–100, com decaimento), independente do período escolhido
        Integer forca) {}
