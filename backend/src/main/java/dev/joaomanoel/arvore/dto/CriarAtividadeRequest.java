package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarAtividadeRequest(@NotBlank String nome) {}
