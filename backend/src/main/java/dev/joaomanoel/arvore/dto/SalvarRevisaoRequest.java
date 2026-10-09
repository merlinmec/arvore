package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SalvarRevisaoRequest(@NotBlank @Size(max = 4000) String nota) {}
