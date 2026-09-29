package dev.joaomanoel.arvore.dto;

import java.util.UUID;

public record ItemHojeDTO(
        UUID atividadeId,
        String atividadeNome,
        UUID objetivoId,
        String objetivoDescricao,
        UUID areaId,
        String areaNome,
        String areaCorTema,
        boolean concluido,
        int streakAtual,
        // Só nas atividades com meta semanal: a meta e quantas vezes já foram feitas na semana até o dia.
        Integer metaSemanal,
        Integer feitasNaSemana) {}
