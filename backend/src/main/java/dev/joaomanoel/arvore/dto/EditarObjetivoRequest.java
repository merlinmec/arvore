package dev.joaomanoel.arvore.dto;

import dev.joaomanoel.arvore.entity.StatusObjetivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EditarObjetivoRequest(
        @NotBlank String descricao,
        LocalDate prazo,
        @NotNull StatusObjetivo status,
        @Positive BigDecimal metaValor,
        @Size(max = 20) String metaUnidade) {}
