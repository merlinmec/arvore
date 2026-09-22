package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CriarObjetivoRequest(@NotBlank String descricao, LocalDate prazo) {}
