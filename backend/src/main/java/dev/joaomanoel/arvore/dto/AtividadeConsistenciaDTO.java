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
        int maiorSequencia) {}
