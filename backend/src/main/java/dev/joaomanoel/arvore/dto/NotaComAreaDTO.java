package dev.joaomanoel.arvore.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotaComAreaDTO(
        UUID id,
        UUID areaId,
        String areaNome,
        String titulo,
        String corpo,
        List<String> tags,
        Instant atualizadoEm) {}
