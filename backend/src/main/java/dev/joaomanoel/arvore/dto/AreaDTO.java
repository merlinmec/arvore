package dev.joaomanoel.arvore.dto;

import java.util.UUID;

public record AreaDTO(
        UUID id,
        UUID areaPaiId,
        String nome,
        String descricaoGeral,
        String corTema,
        String wallpaperUrl,
        int ordem) {}
