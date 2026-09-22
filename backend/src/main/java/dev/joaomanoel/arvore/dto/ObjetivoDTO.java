package dev.joaomanoel.arvore.dto;

import dev.joaomanoel.arvore.entity.StatusObjetivo;
import java.time.LocalDate;
import java.util.UUID;

public record ObjetivoDTO(
        UUID id, UUID areaId, String descricao, LocalDate prazo, StatusObjetivo status) {}
