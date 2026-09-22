package dev.joaomanoel.arvore.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NotaDTO(
        UUID id,
        UUID areaId,
        String titulo,
        String corpo,
        List<String> tags,
        Instant criadoEm,
        Instant atualizadoEm) {}
