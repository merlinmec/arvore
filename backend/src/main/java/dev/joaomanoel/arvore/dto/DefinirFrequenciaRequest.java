package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DefinirFrequenciaRequest(@Min(1) @Max(7) int vezes) {}
