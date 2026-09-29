package dev.joaomanoel.arvore.dto;

import dev.joaomanoel.arvore.entity.DiaSemana;
import java.util.List;
import java.util.UUID;

/** {@code vezesPorSemana} vem preenchido quando a atividade segue uma meta semanal em vez de dias fixos. */
public record AtividadeDTO(
        UUID id, UUID objetivoId, String nome, List<DiaSemana> diasAgendados, Integer vezesPorSemana) {}
