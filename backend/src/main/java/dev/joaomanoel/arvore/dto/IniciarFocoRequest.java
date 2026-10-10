package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IniciarFocoRequest(@NotNull UUID atividadeId) {}
