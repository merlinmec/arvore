package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Substitui o registro do dia inteiro: campo ausente é apagado. */
public record SalvarDiarioRequest(
        @Min(1) @Max(5) Integer humor, @Min(1) @Max(5) Integer energia, @Size(max = 4000) String nota) {}
