package dev.joaomanoel.arvore.dto;

import java.math.BigDecimal;
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
        Integer feitasNaSemana,
        // Só quando o objetivo tem meta quantitativa: a unidade e o valor registrado nesse dia.
        String metaUnidade,
        BigDecimal valor) {}
