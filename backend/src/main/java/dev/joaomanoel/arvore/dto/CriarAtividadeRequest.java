package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code negativa} cria um hábito negativo ("não fumar"); ausente vale como falso. */
public record CriarAtividadeRequest(@NotBlank String nome, Boolean negativa) {}
