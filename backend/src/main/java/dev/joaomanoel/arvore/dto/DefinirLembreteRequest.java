package dev.joaomanoel.arvore.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** Horário do lembrete, como "07:30". */
public record DefinirLembreteRequest(@NotNull LocalTime horario) {}
