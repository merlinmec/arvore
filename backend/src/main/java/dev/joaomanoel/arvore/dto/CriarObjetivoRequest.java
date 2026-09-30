package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CriarObjetivoRequest(
        @NotBlank String descricao,
        LocalDate prazo,
        @Positive BigDecimal metaValor,
        @Size(max = 20) String metaUnidade) {}
