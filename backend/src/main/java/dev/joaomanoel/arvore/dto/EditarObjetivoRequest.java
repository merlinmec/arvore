package dev.joaomanoel.arvore.dto;

import dev.joaomanoel.arvore.entity.StatusObjetivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EditarObjetivoRequest(
        @NotBlank String descricao, LocalDate prazo, @NotNull StatusObjetivo status) {}
