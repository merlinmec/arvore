package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotBlank;

public record EditarAreaRequest(
        @NotBlank String nome, String descricaoGeral, String corTema, String wallpaperUrl) {}
