package dev.joaomanoel.arvore.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tempo de foco de um período: total, dia a dia, por galho da árvore e por atividade. */
public record TempoFocoDTO(
        LocalDate inicio,
        LocalDate fim,
        int totalMinutos,
        int sessoes,
        List<DiaDTO> dias,
        List<AreaDTO> porArea,
        List<AtividadeDTO> atividades) {

    public record DiaDTO(LocalDate data, int minutos) {}

    public record AreaDTO(UUID areaId, String nome, String corTema, int minutos) {}

    public record AtividadeDTO(
            UUID atividadeId, String nome, String areaNome, String areaCorTema, int minutos, int sessoes) {}
}
