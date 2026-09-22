package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CriarAreaRequest(
        @NotBlank String nome,
        UUID areaPaiId,
        String descricaoGeral,
        String corTema,
        String wallpaperUrl) {}
