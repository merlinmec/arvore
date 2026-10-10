package dev.joaomanoel.arvore.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record ItemHojeDTO(
        UUID atividadeId,
        String atividadeNome,
        UUID objetivoId,
        String objetivoDescricao,
        UUID areaId,
        String areaNome,
        String areaCorTema,
        // Há registro nesse dia. Num hábito negativo o registro é a recaída, então cumprido é o contrário.
        boolean concluido,
        boolean negativa,
        int streakAtual,
        // Só nas atividades com meta semanal: a meta e quantas vezes já foram feitas na semana até o dia.
        Integer metaSemanal,
        Integer feitasNaSemana,
        // Só quando o objetivo tem meta quantitativa: a unidade e o valor registrado nesse dia.
        String metaUnidade,
        BigDecimal valor,
        // horário do lembrete, se a atividade tiver um
        LocalTime lembrete) {}
