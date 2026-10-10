package dev.joaomanoel.arvore.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Retrato completo dos dados, pra backup e restauração. Guarda ids e datas de criação como estão no
 * banco: a consistência depende delas (nenhum dia antes da criação da atividade conta como
 * planejado), então restaurar não pode reescrevê-las.
 */
public record BackupDTO(
        Integer versao,
        Instant geradoEm,
        List<Area> areas,
        List<Nota> notas,
        List<Objetivo> objetivos,
        List<Atividade> atividades,
        List<Agenda> agendas,
        List<Frequencia> frequencias,
        List<Conclusao> conclusoes,
        List<Revisao> revisoes,
        List<Diario> diario) {

    /** Versão do formato; sobe quando um campo muda de sentido ou vira obrigatório. */
    public static final int VERSAO_ATUAL = 1;

    public record Area(
            UUID id,
            UUID areaPaiId,
            String nome,
            String descricaoGeral,
            String corTema,
            String wallpaperUrl,
            Integer ordem,
            Instant criadoEm,
            Instant atualizadoEm) {}

    public record Nota(
            UUID id, UUID areaId, String titulo, String corpo, List<String> tags, Instant criadoEm, Instant atualizadoEm) {}

    public record Objetivo(
            UUID id,
            UUID areaId,
            String descricao,
            LocalDate prazo,
            String status,
            BigDecimal metaValor,
            String metaUnidade,
            Instant criadoEm,
            Instant atualizadoEm) {}

    public record Atividade(
            UUID id, UUID objetivoId, String nome, Boolean negativa, Instant criadoEm, Instant atualizadoEm) {}

    public record Agenda(
            UUID id, UUID atividadeId, String diaSemana, LocalDate vigenteDe, LocalDate vigenteAte, Instant criadoEm) {}

    public record Frequencia(
            UUID id, UUID atividadeId, Integer vezes, LocalDate vigenteDe, LocalDate vigenteAte, Instant criadoEm) {}

    public record Conclusao(UUID id, UUID atividadeId, LocalDate data, BigDecimal valor, Instant criadoEm) {}

    public record Revisao(UUID id, LocalDate semana, String nota, Instant criadoEm, Instant atualizadoEm) {}

    public record Diario(
            UUID id,
            LocalDate data,
            Integer humor,
            Integer energia,
            String nota,
            Instant criadoEm,
            Instant atualizadoEm) {}
}
