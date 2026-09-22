package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CriarNotaRequest(@NotBlank String titulo, String corpo, List<String> tags) {}
