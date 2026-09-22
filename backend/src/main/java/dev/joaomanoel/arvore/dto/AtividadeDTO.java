package dev.joaomanoel.arvore.dto;

import dev.joaomanoel.arvore.entity.DiaSemana;
import java.util.List;
import java.util.UUID;

public record AtividadeDTO(UUID id, UUID objetivoId, String nome, List<DiaSemana> diasAgendados) {}
