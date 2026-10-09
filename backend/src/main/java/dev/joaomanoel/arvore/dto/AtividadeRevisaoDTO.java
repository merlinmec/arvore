package dev.joaomanoel.arvore.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Uma atividade na revisão da semana. {@code atividade} traz a grade atual, pra ajustar a próxima
 * semana ali mesmo; {@code diasPerdidos} são os dias passados em que algo planejado escapou (hoje
 * ainda pode ser feito, então não entra).
 */
public record AtividadeRevisaoDTO(
        AtividadeDTO atividade,
        String objetivoDescricao,
        UUID areaId,
        String areaNome,
        String areaCorTema,
        int planejado,
        int concluido,
        List<LocalDate> diasPerdidos) {}
